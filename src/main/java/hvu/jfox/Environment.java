package hvu.jfox;

import java.util.HashMap;
import java.util.Map;

class DefinedVariable {
    private Object value;
    private final boolean editable;

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

    void define(String name, Object value, boolean editable) {
        values.put(name, new DefinedVariable(value, editable));
    }

    void define(String name, Object value) {
        values.put(name, new DefinedVariable(value, true));
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

    boolean has(String name) {
        return values.containsKey(name);
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

    Environment ancestor(Integer distance) {
        Environment env = this;
        for (int i = 0; i < distance; i++) {
            env = env.enclosing;
        }

        return env;
    }

    public Object getAt(Integer distance, Token name) {
        return ancestor(distance).get(name);
    }

    public Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name).getValue();
    }

    public void assignAt(Integer distance, Token name, Object value) {
        ancestor(distance).define(name.lexeme, value);
    }
}
