# OOP and Class
There are multiple ways to implement OOP, it can be either class (Java, Python) or prototypes (JavaScript) or [multimethods](https://en.wikipedia.org/wiki/Multiple_dispatch).

In summary, multimethods is when we can use the same name for multiple functions with different arguments' types, in this case it usually needs to take arguments into account in order to determine which function is being called. This is contrast to `single-dispatch`.

## See class in another perspective
In the [Crafting Interpreters Guide - Classes](https://www.craftinginterpreters.com/classes.html), the author created hashmaps to store properties and functions, I think this can be seen as an `Environment` within a class, which create a reusable object - a `class`. 
```
// create an block
{
    var a = 1;
    
    function increase(number) {
        return number + 1;
    }
}

// create a class
class Example {
    var a = 1;
    
    increase(number) {
        return number + 1;
    }
}
```

## `this` expression
- `this` is an expression because it will resolve to another value which is the instance itself.
```
class Human {
    speak() {
        this.name = "Huy" // this -> current human instance -> FoxInstance
    }
}
```
- In Fox Language, an `instance` get `bind` to a method on get, which means a new environment will create with a new variable named `this` attach to the instance. 
```
var h = Human()
h.speak();
// h.speak is the `get` action, where we `get` the method and `bind` the instance to the method and return a new Function and environment
// then `()` call the method with the new environment
```
