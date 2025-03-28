package hvu.jfox;

import java.util.List;
import java.util.Map;

public class FoxClass implements FoxCallable {
    private final Map<String, FoxFunction> methods;
    final String name;

    public FoxClass(String name, Map<String, FoxFunction> methods) {
        this.methods = methods;
        this.name = name;
    }

    @Override
    public String toString() {
        return "<Class: " + this.name + ">";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        FoxInstance instance = new FoxInstance(this);
        FoxFunction constructor = getMethodByName("constructor");
        if(constructor != null) {
            constructor.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        FoxFunction initializer = getMethodByName("constructor");

        if(initializer != null) {
            return initializer.arity();
        }

        return 0;
    }

    public FoxFunction getMethodByName(String name) {
        return methods.get(name);
    }
}
