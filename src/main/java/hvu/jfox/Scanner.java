package hvu.jfox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0; // First char of the lexeme
    private int current = 0; // Current char to be consider
    private int line = 1; // Line of current char

    private static final Map<String, TokenType> keywords = new HashMap<String, TokenType>() {{
        put("and", TokenType.AND);
        put("class", TokenType.CLASS);
        put("else", TokenType.ELSE);
        put("false", TokenType.FALSE);
        put("for", TokenType.FOR);
        put("function", TokenType.FUNCTION);
        put("if", TokenType.IF);
        put("nil", TokenType.NIL);
        put("or", TokenType.OR);
        put("print", TokenType.PRINT);
        put("return", TokenType.RETURN);
        put("super", TokenType.SUPER);
        put("this", TokenType.THIS);
        put("true", TokenType.TRUE);
        put("var", TokenType.VAR);
        put("const", TokenType.CONST);
        put("while", TokenType.WHILE);
        put("break", TokenType.BREAK);
        put("continue", TokenType.CONTINUE);
    }};

    public Scanner(String source) {
        this.source = source;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(Token.make(type, text, literal, line));
    }

    private boolean match(char expectedChar) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expectedChar) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void scanString() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Fox.error(line, "Unterminated string");
            return;
        }

        // Consume the closing "
        advance();

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private void scanNumber() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            do advance();
            while (isDigit(peek()));
        }

        String value = source.substring(start, current);
        addToken(TokenType.NUMBER, Double.parseDouble(value));
    }

    private void scanIdentifier() {
        while (isAlphaNumeric(peek())) advance();
        String value = source.substring(start, current);

        TokenType type = keywords.get(value);

        if(type == null) type = TokenType.IDENTIFIER;

        addToken(type);
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '(':
                addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(TokenType.RIGHT_PAREN);
                break;
            case '{':
                addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                addToken(TokenType.RIGHT_BRACE);
                break;
            case ',':
                addToken(TokenType.COMMA);
                break;
            case '.':
                addToken(TokenType.DOT);
                break;
            case '-':
                addToken(TokenType.MINUS);
                break;
            case '+':
                addToken(TokenType.PLUS);
                break;
            case ';':
                addToken(TokenType.SEMICOLON);
                break;
            case '*':
                addToken(TokenType.STAR);
                break;
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '/':
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;
            case '"':
                scanString();
                break;
            default:
                if (isDigit(c)) {
                    scanNumber();
                } else if (isAlpha(c)) {
                    scanIdentifier();
                } else {
                    Fox.error(line, "Unexpected input character");
                }
                break;
        }
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        this.tokens.add(Token.make(TokenType.EOF, "", null, line));
        return this.tokens;
    }
}
