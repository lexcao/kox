# Kox

`Kotlin` implementation of `Lox` language
from [Crafting Interpreter](https://github.com/munificent/craftinginterpreters)

# Usage

Firstly, install binary script.

```bash
$ ./gradlew installDist
```

Then, just execute in REPL.

```bash
$ ./build/install/kox/bin/kox
> print "hello kox";
hello kox
```

Additionally, execute via file.

```bash
$ ./build/install/kox/bin/kox {script}
```
