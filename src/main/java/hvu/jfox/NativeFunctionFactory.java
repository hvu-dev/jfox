package hvu.jfox;

import java.util.HashMap;
import java.util.List;

public class NativeFunctionFactory {
    static HashMap<String, FoxCallable> createAll() {
        HashMap<String, FoxCallable> nativeFunctions = new HashMap<>();

        nativeFunctions.put("clock", createClockCallable());
        nativeFunctions.put("print", createPrintCallable());
        return nativeFunctions;
    }

    private static FoxCallable createClockCallable() {
        return new FoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<Built-in clock function>";
            }
        };
    }

    private static FoxCallable createPrintCallable() {
        return new FoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                for(Object argument: arguments) {
                    System.out.print(argument.toString());
                }
                return null;
            }

            @Override
            public int arity() {
                return 255;
            }
        };
    }
}
