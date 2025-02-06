package hvu.jfox;

class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}

public class Interpreter implements Expr.Visitor<Object> {
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case TokenType.GREATER -> {
                return (double) left > (double) right;
            }
            case TokenType.GREATER_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                return (double) left >= (double) right;
            }
            case TokenType.LESS -> {
                checkNumberOperand(expr.operator, left, right);
                return (double) left < (double) right;
            }
            case TokenType.LESS_EQUAL -> {
                checkNumberOperand(expr.operator, left, right);
                return (double) left <= (double) right;
            }
            case TokenType.EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case TokenType.BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case TokenType.PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String || right instanceof String) {
                    return String.valueOf(left) + String.valueOf(right);
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            }
            case TokenType.MINUS -> {
                checkNumberOperand(expr.operator, left, right);
                return (double) left - (double) right;
            }
            case TokenType.STAR -> {
                checkNumberOperand(expr.operator, left, right);
                return (double) left * (double) right;
            }
            case TokenType.SLASH -> {
                checkNumberOperand(expr.operator, left, right);
                if((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Zero division error: division must not be 0.");
                }
                return (double) left / (double) right;
            }
            default -> {
                return null;
            }
        }
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            }
            case TokenType.BANG -> {
                return !isTruthy(right);
            }
            default -> {
                return null;
            }
        }
    }

    private boolean isTruthy(Object object) {
        return switch (object) {
            case null -> false;
            case String s when object.equals("") -> false;
            case Boolean b -> b;
            default -> true;
        };
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isEqual(Object first, Object second) {
        if (first == null && second == null) return true;
        if (first == null) return false;

        return first.equals(second);
    }

    private void checkNumberOperand(Token operator, Object value) {
        if (!(value instanceof Double)) {
            throw new RuntimeError(operator, "Operand must be a number");
        }
    }

    private void checkNumberOperand(Token operator, Object first, Object second) {
        if (first instanceof Double && second instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Main.runtimeError(error);
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}
