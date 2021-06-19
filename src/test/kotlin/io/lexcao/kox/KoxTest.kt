package io.lexcao.kox

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KoxTest {

    @AfterEach
    fun cleanup() {
        Kox.clearErrors()
    }

    @Test
    fun scopeVariables() {
        val source = """
            var a = "global a";
            var b = "global b";
            var c = "global c";
            {
              var a = "outer a";
              var b = "outer b";
              {
                var a = "inner a";
                print a;
                print b;
                print c;
                print "---";
              }
              print a;
              print b;
              print c;
              print "---";
            }
            print a;
            print b;
            print c;
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun conditional() {
        val source = """
            print "hi" or 2;
            print nil or "yes";
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun controlFlow() {
        val source = """
            var a = 0;
            var temp;

            for (var b = 1; a < 10000; b = temp + b) {
              print a;
              temp = a;
              a = b;
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun loopWhile() {
        val source = """
            var i = 0;
            while (i < 20) {
                print i;
                i = i + 1;
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun loopFor() {
        val source = """
            for (var i = 0; i < 20;
            i = i + 1) {
                print i;
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun loopForDeSugar() {
        val source = """
            {
                var i = 0;
                while (i < 20) {
                    {
                        print i;
                    }
                    i = i + 1;
                }
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun function() {
        val source = """
            fun sayHi(first, last) {
                print "Hi, " + first + " " + last + "!";
            }

            sayHi("Dear", "Reader"); 
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun functionWithReturn() {
        val source = """
            fun fib(n) {
               if (n <= 1) return n;
               return fib(n - 2) + fib(n - 1);
            }
            
            for (var i = 0; i < 20; 
            i = i + 1) {
                print fib(i);
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun functionWithClosure() {
        val source = """
            fun makeCounter() {
              var i = 0;
              fun count() {
                i = i + 1;
                print i;
              }

              return count;
            }

            var counter = makeCounter();
            counter(); // "1".
            counter(); // "2".
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun scope() {
        val source = """
            var a = "global";
            {
              fun showA() {
                print a;
              }

              showA();
              var a = "block";
              showA();
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun scopeError() {
        val source = """
            fun bad() {
                var a = "first";
                var a = "second";
                
                var b = 10;
                b = b;
            }
            bad();
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun invalidReturn() {
        val source = """
            return "at top level";
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun classDecl() {
        val source = """
            class DevonshireCream {
              serveOn() {
                return "Scones";
              }
            }

            print DevonshireCream; // Prints "DevonshireCream".
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun classInstance() {
        val source = """
            class Bagel {}
            var bagel = Bagel();
            print bagel; // Prints "Bagel.instance".
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun classMethods() {
        val source = """
            class Foo {
                eat() {
                    print "Crunch crunch crunch!";
                }
            }
            Foo().eat();
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun classThis() {
        val source = """
            class Egotist {
                speak() { print this; }
            }
            var method = Egotist().speak;
            method();
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun classThis2() {
        val source = """
            class Cake {
              taste() {
                var adjective = "delicious";
                print "The " + this.flavor + " cake is " + adjective + "!";
              }
            }

            var cake = Cake();
            cake.flavor = "German chocolate";
            cake.taste(); // Prints "The German chocolate cake is delicious!".
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun classInitializer() {
        val source = """
            class Foo {
                init() {
                    print this;
                }
            }
            var foo = Foo();
            print foo.init();
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun returnInitializer() {
        val source = """
            class Foo {
                init() { return; }
            }
            
            class Bar {
                init() { return "bar"; }
            }
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun callFunctionFromSuperClass() {
        val source = """
            class Doughnut {
                cook() { print "Fry until golden brown"; }
            }
            class BoostCream < Doughnut {
                cook() {
                    super.cook();
                    print "Pipe full of custard and coat with chocolate";
                }
            }
            BoostCream().cook();
        """.trimIndent()
        Kox.run(source)
    }

    @Test
    fun invalidSuperCall() {
        val source = """
            class Ecalir {
                cook() {
                    super.cook();
                    print "ecalir";
                }
            }
        """.trimIndent()
        Kox.run(source)
    }
}
