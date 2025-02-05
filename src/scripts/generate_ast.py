FILE_NAME = "Expr.java"
BASE_CLASS_NAME = "Expr"
EXPR_TYPES = {
    "Binary": {
        "args": [
            {"type": "Expr", "name": "left"},
            {"type": "Token", "name": "operator"},
            {"type": "Expr", "name": "right"},
        ]
    },
    "Grouping": {"args": [{"type": "Expr", "name": "expression"}]},
    "Literal": {
        "args": [
            {"type": "Object", "name": "value"},
        ]
    },
    "Unary": {
        "args": [
            {"type": "Token", "name": "operator"},
            {"type": "Expr", "name": "right"},
        ]
    },
}


def define_types(types: dict):
    code = ""
    for class_name, value in types.items():
        code += "static class " + class_name + " extends " + BASE_CLASS_NAME + " {"
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
            + BASE_CLASS_NAME
            + "(this); }"
        )

        code += "}"

    return code


def define_visitors(types: dict):
    code = "interface Visitor<R> {"
    for class_name, value in types.items():
        code += (
            "R visit"
            + class_name
            + BASE_CLASS_NAME
            + "("
            + class_name
            + " "
            + BASE_CLASS_NAME.lower()
            + ");"
        )

    code += "}"
    return code


def main(path=None):
    file_path = path or f"src/main/java/hvu/jfox/{FILE_NAME}"
    with open(file_path, "w+") as f:
        f.writelines(
            [
                "package hvu.jfox;\n",
                "import java.util.List;\n\n",
                "abstract class " + BASE_CLASS_NAME + " {\n",
            ]
        )
        f.write("\nabstract <R> R accept(Visitor<R> visitor);")

        f.writelines(define_types(EXPR_TYPES))
        f.writelines(define_visitors(EXPR_TYPES))
        f.write("}\n")


if __name__ == "__main__":
    main()
