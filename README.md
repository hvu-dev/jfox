## Scanner
- Error reporting should be separated from the source
- Add ErrorReporter with interface to each component

While scanning we should have the following functions:
- advance: return the current character and increase the current position
- match: check current token if it matches our desire, if matched, increase the current position
- peek: go to the end of the string

- Scan string and number should expect ending/leading characters to be correct 
- For keyword and identifier, we should assume it is identifier and then compare it with built-in keywords (hash table), then process the key (add key).

- Arithmetic evaluation may use post-order traversal
- Tokens can be defined using regular language, but it's not powerful enough to handle expressions which can nest arbitrarily deeply.

- Formal languages are defined using a fixed set of rules
- BNF notation can use to define CFG (Type 2 grammar in Discrete math)
```
Notation
expression     → literal
               | unary
               | binary
               | grouping ;

literal        → NUMBER | STRING | "true" | "false" | "nil" ;
grouping       → "(" expression ")" ;
unary          → ( "-" | "!" ) expression ;
binary         → expression operator expression ;
operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
               | "+"  | "-"  | "*" | "/" ;
               
Note: Unary is the operation performs on itself (f: A -> A, where f is a unary operation on A). Example: -3, !true, n!
```
- Abstract Syntax Tree: as grammar is recursive, we can express it in the tree data structure
- Expression problem: functional vs object-oriented. Functional programming language, you have to add functions for each type, hence adding a new Type is challenging. In contrast, adding a new Type in OO lang is pretty easy, however adding a function to that type force us to implement it for all existing types. See more [Expressing Problem](https://en.wikipedia.org/wiki/Expression_problem). 

## Parser
- After converted source code to a series of tokens, parser will convert tokens in to a richer representation.
- Ambiguous of syntax tree: different grouping order may result in different syntax tree, and different final result hence it should follow specific rules `PRECEDENCE` and `ASSOCIATIVITY` (Read more: [Operator Associativity](https://en.wikipedia.org/wiki/Operator_associativity))
```
Precedence rules in C

Name         |   Operators  |	Associates
Equality     |   == !=      |   Left
Comparison   |   > >= < <=  |   Left
Term	     |   - +	    |   Left
Factor	     |   / *	    |   Left
Unary	     |   ! -	    |   Right
```
- Associativity: determines which operator is evaluated first in a series of the same operator

- Example: 
  - `5 - 3 - 1` should be evaluated as `(5 - 3) - 1` because `-` is Left-Associative (in term)
  - `!!true` should be evaluated as !(!true) becase `!` is Right-Associative
  - `---3` should be evaluated as `-(-(-3))` because `-` is Right-Associative (in Unary)
  - A detail example: [Wikipedia example](https://en.wikipedia.org/wiki/Operator_associativity#A_detailed_example)
```
expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
```
- Parsing techniques: Recursive descent, parser combinators, Earley parsers, the shunting yard algorithm, and packrat parsing
- Recursive descent is a top-down (and left to right) parser (can be considered as the simplest), which means it starts from outermost grammar rule, and finally reaching the leaves in AST. 
```
Example: match 5 == 3
                          expression   (0)
                              ↓
            comparison ( ( "!=" | "==" ) comparison )*    (1)
                              ↓
            term ( ( ">" | ">=" | "<" | "<=" ) term )*    (2)
                              ↓
                factor ( ( "-" | "+" ) factor )*    (3)
                              ↓
                unary ( ( "/" | "*" ) unary )*    (4)
              ↙                               ↘
( "!" | "-" ) unary   (5)                    primary   (6)
                                                ↘
                                NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")"   (7)
```
- To evaluate expressions in dynamic type language, we need a "type" that can hold every value which is `Object` in Java, and `pointer` in C/C++
- We can use Visitor Pattern and Interpreter pattern (inspired by Composite Pattern) to decouple the action on expression from the expression class.
```
Visitor Pattern
            accept
Expression -------> Vistor
            
            call
Expression -------> Vistor action

Only the expression itself knows which action they should take, hence it will call the correct action which will be declared by the "Visitor" class.   
```
- Many static-typed languages (Like Java) perform type check at run-time. It allows more flexibility while still keeping the integrity of the data. In general, types info are usually kept within compiler/interpreter itself rather than regarding memory/instructions specific. As compiler and interpreter are well aware of memory layout, they can easily retrieve the data from memory and then load it to a variable of a specific type (which is defined by the compiler/interpreter). Read more: [How do variables in C++ store their type?](https://softwareengineering.stackexchange.com/a/380349) and [Type Safety](https://en.wikipedia.org/wiki/Type_safety).
- Expression is what produces value, Statement is only for declaration (perform an action). Expression can be a part of statement, but not the way around. 
```
var a = 1 + 2;
       |-----|
          ↑  
      expression -> produce a value
|-------------|
       ↑
    statement -> perform an assignment (a side-effect)
```
- Everytime we add a new syntax (Token/Expr/Stmt/etc), we're going to update the grammar rules.
```
program        → statement* EOF ;
statement      → exprStmt | printStmt ;
exprStmt       → expression ";" ;
printStmt      → "print" expression ";" ;
```
- Starting rule of this language is `program`, somewhat similar to a starting rule of Python `file: [statements] ENDMARKER` ([Python's Grammar](https://docs.python.org/3/reference/grammar.html))
- Declaration statements are a little different from ordinary statements, now we add a distinguish statement type for it
```
program        → declaration* EOF ;
declaration    → varDecl | statement ;
statement      → exprStmt | printStmt ;
varDecl        → ("var" | "const") IDENTIFIER ( "=" expression )? ";" ;
```
- When we have statement and declaration, we can assign a variable to an identifier and use that identifier as an expression that produce value. This grammar belongs to `primary`, the same category with `NUMBER`, `STRING`.
```
primary        → "true" | "false" | "nil"
               | NUMBER | STRING
               | "(" expression ")"
               | IDENTIFIER ;
```
### Assignment
```
expression     → assignment ;
assignment     → IDENTIFIER "=" assignment | equality ;
```
- In this language and C-derived languages, assignment is an expression while in Pascal/Python/Go, it's a statement.
- Parser will not recognise a `l-value` until it get to `=` token.
- Lexical scope (static scope): text in the program itself shows where a scope begins and ends >< dynamic scope: don't know what a name refers to until you execute the code.
- Shadowing: a variable should stay within it scope without overriding the outer scope
```
var a = 1;
{
  var a = 2;
  print a; // should print 2
}
print a; // should print 1;
```
- Block statement: surrounded by curly braces (can be an empty block)
```
statement      → exprStmt | printStmt | block ;
block          → "{" declaration* "}" ;
```
### Control flow
```
statement      → exprStmt | ifStmt | printStmt | block ;
ifStmt         → "if" "(" expression ")" statement
               ( "else" statement )? ;
```
- Short-circuit: evaluate left to right, as soon as we can guarantee the result of the logic we can go directly to evaluate the statements/expressions inside
- In C, `||` (or) has the lower precedence than `&&` (and)
```
expression     → assignment ;
assignment     → IDENTIFIER "=" assignment
               | logic_or ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
```
- The logical here is pretty similar to `disjunction` and `conjunction` from Python grammar.
```
statement      → exprStmt
               | ifStmt
               | printStmt
               | whileStmt
               | block ;

whileStmt      → "while" "(" expression ")" statement ;
```
### Error recovery
- The parser recognise there is something wrong with the current token, it remembers that and then continue to go on to seek for any next possible error called: `error recovery`.
- Runtime Environment: where all the identifier and memory are mapped.

### Turing machine
- We can not compute all functions and can not prove all statements are true ([Computable functions](https://en.wikipedia.org/wiki/Computable_function), [Church-Turing thesis](https://en.wikipedia.org/wiki/Church%E2%80%93Turing_thesis))

