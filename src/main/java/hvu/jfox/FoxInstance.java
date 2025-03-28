package hvu.jfox;

import java.util.HashMap;

public class FoxInstance {
    public FoxClass klass;
    private final HashMap<String, Object> fields = new HashMap<>();

    FoxInstance(FoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return "<Instance: " + this.klass.name + ">";
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        FoxFunction method = this.klass.getMethodByName(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "' on " + this.klass.name + " instance");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
