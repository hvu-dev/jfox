package hvu.jfox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

enum FunctionType {
    FUNCTION {
        @Override
        public String toString() {
            return "function";
        }
    },
    METHOD {
        @Override
        public String toString() {
            return "method";
        }
    }
}

public class Parser {
    private final int MAX_FUNCTION_ARGUMENTS = 255;

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    /* Grammar functions */
    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) return classDeclaration();
            if (match(TokenType.FUNCTION)) return functionDeclaration(FunctionType.FUNCTION);
            if (match(TokenType.VAR, TokenType.CONST)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect class name");
        consume(TokenType.LEFT_BRACE, "Expect '{' before class body");

        List<Stmt.Function> methods = new ArrayList<>();
        while(!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            methods.add(functionDeclaration(FunctionType.METHOD));
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' before class body");
        return new Stmt.Class(name, methods);
    }

    private Stmt.Function functionDeclaration(FunctionType type) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + type.toString() + " name.");
        consume(TokenType.LEFT_PAREN, "Expect left parenthesis after" + type.toString() + " name.");

        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= MAX_FUNCTION_ARGUMENTS) {
                    error(peek(), type.toString() + "can't have more than" + MAX_FUNCTION_ARGUMENTS + "parameters");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name"));
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN, "Expect right parenthesis after parameters");
        consume(TokenType.LEFT_BRACE, "Expect '{' before" + type.toString() + "body");

        List<Stmt> body = blockStatement();

        return new Stmt.Function(name, parameters, body);
    }

    private Stmt varDeclaration() {
        boolean editable = previous().type != TokenType.CONST;

        Token name = consume(TokenType.IDENTIFIER, "Expected variable name.");
        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer, editable);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.BREAK)) return breakStatement();
        if (match(TokenType.CONTINUE)) return continueStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(blockStatement());

        return expressionStatement();
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Invalid syntax: missing left parenthesis for if statement");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Invalid syntax: missing right parenthesis for if statement");
        // TODO: Force it to be blockStatement
        Stmt ifBlock = statement();
        Stmt elseBlock = null;

        if (match(TokenType.ELSE)) {
            // TODO: Force it to be blockStatement
            elseBlock = statement();
        }

        return new Stmt.If(condition, ifBlock, elseBlock);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' for `for` statement");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR, TokenType.CONST)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition");
        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) condition = new Expr.Literal(true);

        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' for while statement");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' for while statement");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private List<Stmt> blockStatement() {
        List<Stmt> statements = new ArrayList<Stmt>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block");
        return statements;
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr expr = null;
        if(!check(TokenType.SEMICOLON)) {
            expr = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return's expression");
        return new Stmt.Return(keyword, expr);
    }

    private Stmt breakStatement() {
        consume(TokenType.SEMICOLON, "Expect ';' after break");
        return new Stmt.Break(previous());
    }

    private Stmt continueStatement() {
        consume(TokenType.SEMICOLON, "Expect ';' after continue");
        return new Stmt.Continue(previous());
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get get) {
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr left = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            return new Expr.Logical(left, operator, right);
        }

        return left;
    }

    private Expr and() {
        Expr left = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            return new Expr.Logical(left, operator, right);
        }

        return left;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if(match(TokenType.DOT)) {
                Token name = consume(TokenType.IDENTIFIER, "Expected property's name after '.'");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<Expr>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= MAX_FUNCTION_ARGUMENTS) {
                    // Maybe add a warning function
                    error(peek(), "Maximum " + MAX_FUNCTION_ARGUMENTS + "arguments for function exceeded");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after function's arguments");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.STRING, TokenType.NUMBER)) return new Expr.Literal(previous().literal);
        if (match(TokenType.THIS)) return new Expr.This(previous());
        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression");
    }
    /* End grammar  functions */

    /* Utility functions */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private boolean check(TokenType type) {
        // TODO: why do we need to check isAtEnd here?
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) return advance();

        throw error(peek(), errorMessage);
    }

    private ParseError error(Token token, String message) {
        Fox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case TokenType.CLASS:
                case TokenType.FUNCTION:
                case TokenType.VAR:
                case TokenType.FOR:
                case TokenType.IF:
                case TokenType.WHILE:
                case TokenType.PRINT:
                case TokenType.RETURN:
                    return;
            }

            advance();
        }
    }
    /* End utility functions */

}
