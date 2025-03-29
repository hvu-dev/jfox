package hvu.jfox;

import java.util.*;

enum FuncType {
    FUNCTION,
    INITIALIZER,
    METHOD,
    NONE,
    WHILE
}

enum ClassType {
    CLASS,
    NONE
}

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private final Set<String> builtInFunctions = NativeFunctionFactory.builtInFunctionNames();
    private FuncType currentFunctionType = FuncType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Fox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (builtInFunctions.contains(expr.name.lexeme)) {
            // Ignore built-in functions
            return null;
        }

        /* NOT A && NOT B
         * A | B | NOT A | NOT B | NOT A && NOT B
         * T   T   F       F       F
         * T   F   F       T       F
         * F   T   T       F       F
         * F   F   T       T       T
         *
         * NOT(A || B)
         * A | B | OR | NOT (A || B)
         * T   T   T     F
         * T   F   T     F
         * F   T   T     F
         * F   F   F     T
         * */
        if (!(scopes.isEmpty() || isVariableInAnyScope(expr.name.lexeme))) {
            Fox.error(expr.name, "Can not access before initialization");
        }
        // scopes.peek().get(expr.name.lexeme) == Boolean.TRUE
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (currentFunctionType != FuncType.WHILE) {
            Fox.error(stmt.token, "Can not break outside loop");
        }
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        beginScope();
        declare(stmt.name);

        // Why not declare and define right here?
        scopes.peek().put("this", true);
        for (Stmt.Function method : stmt.methods) {
            FuncType localFuncType = FuncType.METHOD;
            if (method.name.lexeme.equals("constructor")) {
                localFuncType = FuncType.INITIALIZER;
            }

            resolveFunction(method, localFuncType);
        }

        define(stmt.name);
        endScope();
        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (currentFunctionType != FuncType.WHILE) {
            Fox.error(stmt.token, "Can not continue outside loop");
        }
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        if (builtInFunctions.contains(stmt.name.lexeme)) {
            Fox.error(stmt.name, "Re-define built-in function");
        }

        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FuncType.FUNCTION);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunctionType == FuncType.NONE) {
            Fox.error(stmt.keyword, "Can not return from top-level code.");
        }
        if (stmt.expression != null) {
            if (currentFunctionType == FuncType.INITIALIZER) {
                Fox.error(stmt.keyword, "Can not return from a non-null value from constructor");
            }
            resolve(stmt.expression);
        }
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);

        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }

        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        FuncType enclosingFunctionType = currentFunctionType;
        currentFunctionType = FuncType.WHILE;

        resolve(stmt.condition);
        resolve(stmt.body);

        currentFunctionType = enclosingFunctionType;
        return null;
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Fox.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void beginScope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    private void endScope() {
        scopes.pop();
    }

    private boolean isVariableInAnyScope(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return true;
            }
        }

        return false;
    }

    public void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt statement) {
        statement.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function stmt, FuncType type) {
        FuncType enclosingFunction = currentFunctionType;
        currentFunctionType = type;
        beginScope();
        for (Token token : stmt.params) {
            declare(token);
            define(token);
        }

        resolve(stmt.body);
        endScope();

        currentFunctionType = enclosingFunction;
    }

    private void resolveLocal(Expr expr, Token name) {
        // Start from the most inner scope.
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }
}
