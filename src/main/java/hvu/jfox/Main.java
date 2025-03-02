package hvu.jfox;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            Fox.runFile(args[0]);
        } else {
            Fox.runPrompt();
        }
    }
}