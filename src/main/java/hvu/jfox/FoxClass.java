package hvu.jfox;

import java.util.List;
import java.util.Map;

public class FoxClass implements FoxCallable {
    private final Map<String, FoxFunction> methods;
    private final FoxClass superclass;
    private final String name;

    public FoxClass(String name, FoxClass superclass, Map<String, FoxFunction> methods) {
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
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

    public String getName() {
        return name;
    }

    public FoxFunction getMethodByName(String name) {
        if(methods.containsKey(name)) {
            return methods.get(name);
        } else if (this.superclass != null) {
            return this.superclass.getMethodByName(name);
        }
        return null;
    }
}
