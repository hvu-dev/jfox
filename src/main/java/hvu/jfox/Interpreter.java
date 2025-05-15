package hvu.jfox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface FoxCallable {
    Object call(Interpreter interpreter, List<Object> arguments);

    int arity();
}


class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}

class StopIteration extends RuntimeError {
    StopIteration(Token token, String message) {
        super(token, message);
    }
}

class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        this.value = value;
    }
}


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final int UNLIMITED_NUMBER_OF_ARGS = -1;
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        defineNativeFunctions();
    }

    private void defineNativeFunctions() {
        for (Map.Entry<String, FoxCallable> entry : NativeFunctionFactory.createAll().entrySet()) {
            globals.define(entry.getKey(), entry.getValue(), false);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            globals.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

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
                if (left instanceof String && right instanceof String) {
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
                if ((double) right == 0) {
                    // Most of the language will throw ZeroDivisionError
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
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof FoxCallable)) {
            throw new RuntimeError(expr.paren, "Expect callable object");
        }

        FoxCallable function = (FoxCallable) callee;
        if (arguments.size() != function.arity() && function.arity() != UNLIMITED_NUMBER_OF_ARGS) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments, got " + arguments.size() + " arguments instead.");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof FoxInstance))
            throw new RuntimeError(expr.name, "Can only access properties from an instance");

        return ((FoxInstance) object).get(expr.name);
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
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        // Short-circuit
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof FoxInstance)) {
            throw new RuntimeError(expr.name, "Non-instance don't have properties");
        }

        Object value = evaluate(expr.value);
        ((FoxInstance) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        FoxClass superclass = (FoxClass) environment.getAt(distance, expr.keyword);
        FoxInstance object = (FoxInstance) environment.getAt(distance - 1, "this");

        FoxFunction method = superclass.getMethodByName(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'");
        }

        return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr, expr.keyword);
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

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr, expr.name);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof FoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
            }
        }

        environment.define(stmt.name.lexeme, null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        Map<String, FoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            FoxFunction function = new FoxFunction(method, environment);
            methods.put(method.name.lexeme, function);
        }
        FoxClass klass = new FoxClass(stmt.name.lexeme, (FoxClass) superclass, methods);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        FoxFunction function = new FoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function, true);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.expression != null) {
            value = evaluate(stmt.expression);
        }

        throw new Return(value);
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (environment.has(stmt.name.lexeme)) {
            Fox.warning(stmt.name, "Re-declare an existing variable");
        }

        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value, stmt.editable);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (StopIteration ex) {
                break;
            }
        }

        return null;
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (Return r) {
            return;
        } catch (RuntimeError error) {
            Fox.runtimeError(error);
        } catch (StackOverflowError error) {
            Fox.runtimeError(error);
        }
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private void execute(Stmt statement) {
        statement.accept(this);
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

    public void resolve(Expr expr, int i) {
        locals.put(expr, i);
    }

    private Object lookupVariable(Expr expr, Token name) {
        Integer distance = locals.get(expr);

        if (distance != null) {
            return environment.getAt(distance, name);
        } else {
            return globals.get(name);
        }
    }
}
