package hvu.jfox;

import java.util.HashMap;
import java.util.Map;

class DefinedVariable {
    private Object value;
    private boolean editable;

    public DefinedVariable(Object value, boolean editable) {
        this.value = value;
        this.editable = editable;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isEditable() {
        return editable;
    }
}

public class Environment {
    final Environment enclosing;
    private final Map<String, DefinedVariable> values = new HashMap<String, DefinedVariable>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, DefinedVariable value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme).getValue();
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            DefinedVariable definedVariable = values.get(name.lexeme);
            if (definedVariable.isEditable()) {
                definedVariable.setValue(value);
                return;
            } else {
                throw new RuntimeError(name, "Cannot re-assign a constant variable: " + name.lexeme + ".");
            }
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
