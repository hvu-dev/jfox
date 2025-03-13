package hvu.jfox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

enum LogLevel {
    WARNING, ERROR
}

public class Fox {
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    private static void run(String input) {
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        interpreter.interpret(statements);
    }

    static void error(int line, String message, LogLevel level) {
        report(line, "", message, level);
    }

    static void error(Token token, String message, LogLevel level) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message, level);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message, level);
        }
    }

    static void warning(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message, LogLevel.WARNING);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message, LogLevel.WARNING);
        }
    }

    private static void report(int line, String where, String message, LogLevel level) {
        String levelText = "";
        if (level == LogLevel.ERROR) {
            levelText = "Error";
        } else if (level == LogLevel.WARNING) {
            levelText = "Warning";
        }

        System.err.println(levelText + " [Line: " + line + "]" + where + ": " + message);
        hadError = true;
    }

    static void runtimeError(RuntimeError error) {
        System.err.println("\n[Line " + error.token.line + "] " + error.getMessage());
        hadRuntimeError = true;
    }

    static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (; ; ) {
            System.out.println("> ");
            String line = reader.readLine();
            if (line == null) break;

            run(line);

            hadError = false;
        }
    }
}
