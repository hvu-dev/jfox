package hvu.jfox;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NativeFunctionFactory {
    static HashMap<String, FoxCallable> createAll() {
        HashMap<String, FoxCallable> nativeFunctions = new HashMap<>();

        nativeFunctions.put("clock", createClockCallable());
        nativeFunctions.put("print", createPrintCallable());
        return nativeFunctions;
    }

    static Set<String> builtInFunctionNames() {
        return Stream.of("clock", "print").collect(Collectors.toSet());
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
                return "<Function clock built-in>";
            }
        };
    }

    private static FoxCallable createPrintCallable() {
        return new FoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                int size = arguments.size();
                for (int i = 0; i < size; i++) {
                    System.out.print(arguments.get(i));

                    if (i != size - 1) {
                        System.out.print(" ");
                    } else {
                        System.out.println();
                    }
                }

                return null;
            }

            @Override
            public int arity() {
                return -1;
            }

            @Override
            public String toString() {
                return "<Function print built-in>";
            }
        };
    }
}
