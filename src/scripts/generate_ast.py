#!/usr/bin/env python3

EXPR_TYPES = {
    "Assign": {
        "args": [
            {"type": "Token", "name": "name"},
            {"type": "Expr", "name": "value"},
        ]
    },
    "Binary": {
        "args": [
            {"type": "Expr", "name": "left"},
            {"type": "Token", "name": "operator"},
            {"type": "Expr", "name": "right"},
        ]
    },
    "Call": {
        "args": [
            {"type": "Expr", "name": "callee"},
            {"type": "Token", "name": "paren"},
            {"type": "List<Expr>", "name": "arguments"},
        ]
    },
    "Get": {"args": [{"type": "Expr", "name": "object"}, {"type": "Token", "name": "name"}]},
    "Grouping": {"args": [{"type": "Expr", "name": "expression"}]},
    "Literal": {
        "args": [
            {"type": "Object", "name": "value"},
        ]
    },
    "Logical": {
        "args": [
            {"type": "Expr", "name": "left"},
            {"type": "Token", "name": "operator"},
            {"type": "Expr", "name": "right"},
        ]
    },
    "Set": {
        "args": [
            {"type": "Expr", "name": "object"},
            {"type": "Token", "name": "name"},
            {"type": "Expr", "name": "value"},
        ]
    },
    "This": {
        "args": [
            {"type": "Token", "name": "keyword"},
        ]
    },
    "Unary": {
        "args": [
            {"type": "Token", "name": "operator"},
            {"type": "Expr", "name": "right"},
        ]
    },
    "Variable": {"args": [{"type": "Token", "name": "name"}]},
}


STMT_TYPES = {
    "Block": {"args": [{"type": "List<Stmt>", "name": "statements"}]},
    "Break": {"args": [{"type": "Token", "name": "token"}]},
    "Class": {"args": [{"type": "Token", "name": "name"}, {"type": "List<Stmt.Function>", "name": "methods"}]},
    "Continue": {"args": [{"type": "Token", "name": "token"}]},
    "Expression": {"args": [{"type": "Expr", "name": "expression"}]},
    "Function": {
        "args": [
            {"type": "Token", "name": "name"},
            {"type": "List<Token>", "name": "params"},
            {"type": "List<Stmt>", "name": "body"},
            {"type": "boolean", "name": "isStatic"},
        ]
    },
    "Return": {
        "args": [
            {"type": "Token", "name": "keyword"},
            {"type": "Expr", "name": "expression"},
        ]
    },
    "If": {
        "args": [
            {"type": "Expr", "name": "condition"},
            {"type": "Stmt", "name": "thenBranch"},
            {"type": "Stmt", "name": "elseBranch"},
        ]
    },
    "Var": {
        "args": [
            {"type": "Token", "name": "name"},
            {"type": "Expr", "name": "initializer"},
            {"type": "boolean", "name": "editable"},
        ]
    },
    "While": {
        "args": [
            {"type": "Expr", "name": "condition"},
            {"type": "Stmt", "name": "body"},
        ]
    },
}

GEN_TYPES = [
#     {"file_name": "Expr.java", "types": EXPR_TYPES, "base_class_name": "Expr"},
    {
        "file_name": "Stmt.java",
        "types": STMT_TYPES,
        "base_class_name": "Stmt",
    },
]


def define_types(types: dict, base_name: str):
    code = ""
    for class_name, value in types.items():
        code += "static class " + class_name + " extends " + base_name + " {"
        args = value["args"]

        args_def = ""
        args_constructor = ""
        constructor = ""
        for arg in args:
            args_def += f"final {arg['type']} {arg['name']};"
            args_constructor += f"{arg['type']} {arg['name']}, "
            constructor += f"this.{arg['name']} = {arg['name']};"

        code += args_def
        args_constructor = args_constructor[: len(args_constructor) - 2]

        code += class_name + "(" + args_constructor + ") {" + constructor + "}"
        code += (
            "@Override <R> R accept(Visitor<R> visitor) {return visitor.visit"
            + class_name
            + base_name
            + "(this); }"
        )

        code += "}"

    return code


def define_visitors(types: dict, base_name: str):
    code = "interface Visitor<R> {"
    for class_name, value in types.items():
        code += (
            "R visit"
            + class_name
            + base_name
            + "("
            + class_name
            + " "
            + base_name.lower()
            + ");"
        )

    code += "}"
    return code


def define_ast(types, base_name, file_name, package_name="hvu.jfox"):
    with open(file_name, "w+") as f:
        f.writelines(
            [
                "package " + package_name + ";\n",
                "import java.util.List;\n\n",
                "abstract class " + base_name + " {\n",
                "\nabstract <R> R accept(Visitor<R> visitor);",
                define_types(types, base_name),
                define_visitors(types, base_name),
                "}\n",
            ]
        )


def main(base_path: str):
    for t in GEN_TYPES:
        define_ast(t["types"], t["base_class_name"], f"{base_path}{t['file_name']}")


if __name__ == "__main__":
    main("src/main/java/hvu/jfox/")
