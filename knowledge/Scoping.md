# Resolve and Binding
## Scoping
There are various type of scoping:
- Lexical scope (static scope): base on the position of the variable in the source code

`var` keyword in JavaScript has lexical scope while `let` and `const` have block scope. 
```
// This is a valid snippet in JavaScript
function incr() {
    function addOne() {
        ++a;
        return a;
    }
    var a = 1; // https://en.wikipedia.org/wiki/JavaScript_syntax#hoisting
    return addOne;
}

const plusOne = incr();
console.log(plusOne());
```

- Dynamic scope: determine by the call stack at runtime
- Scope can be seen as the "environment" between declarations. 
```
// Example:
var a = 1;
// 1
var b = 2;
// 2
```
Basically, in the scope of `a`, there is no `b` yet. Meanwhile in the scope of `b`, there are both `a` and `b`.

In case of self-reference, there are many ways to handle this scenario:
```
var a = 1;
{
    var a = a; 
}
```
1. We can use the initializer `a` value from global scope, then the "new" `a` in the second scope will have the value of 1 which is similar to
2. If we decided to keep the second `a` variable in that scope only, which will result in `a` haven't been declared yet. Then we will need to decide what value we shoud give it, `nil` maybe a reasonable value. 
3. Raise error, since the `a` variable haven't been declared anywhere in that scope. 

Let refer to JavaScript implementation, if we try the similar syntax we will get a result similar to scenario 1. But since ES6, JavaScript introduced block scope with `let` keyword which result in `ReferenceError`. 
```
// Valid
var a = 1;
{
    var a = a;
}

// Invalid: ReferenceError
let a = 1;
{
    let a = a;
}
```
If we try the following syntax in JavaScript, it will run successfully.
```
let a = 1;
{
    console.log(a);
}
```
In contrast, if we use the following syntax, it will fail with ReferenceError again
```
let a = 1;
{
    console.log(a);
    let a = 1;
}
```
So, we can somewhat guess that in the beginning of the block scope, there is no `a` and there is a declaration of `a` within the scope which result in the error. But in the first scenario, it knows that there is no `a` in the scope, so it refers to the outer scope and so on.

Prior to ES6, JavaScript has function scope which has the similar idea to scenario 2. 
```
var a = 1;

function test() {
    console.log(a); // log: undefined
    var a = a; 
}

test();
```
## Resolver pass
A `pass` traverse through the target (ex: the Syntax Tree) and perform actions on them (ex: type check or resolve variable). 
