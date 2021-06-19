package io.lexcao.kox.common

import io.lexcao.kox.component.Environment
import io.lexcao.kox.component.Interpreter

interface KoxCallable {
    val arity: Int get() = 0
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?

    class Function(
        private val declaration: Stmt.Fun,
        private val closure: Environment,
        private val initializer: Boolean = false
    ) : KoxCallable {
        val name: String get() = declaration.identifier.lexeme


        override val arity: Int
            get() = declaration.params.size

        override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
            val environment = Environment(closure)
            declaration.params.forEachIndexed { i, it ->
                environment.define(it, arguments[i])
            }

            try {
                interpreter.executeBlock(declaration.body, Environment(environment))
            } catch (returnValue: Interpreter.Return) {
                if (initializer) return closure.getAt(0, "this")
                return returnValue.value
            }

            if (initializer) return closure.getAt(0, "this")
            return null
        }

        fun bind(instance: Instance): Function {
            val environment = Environment(closure)
            environment.define("this", instance)
            return Function(declaration, environment, initializer)
        }

        override fun toString(): String = "<fn ${declaration.identifier.lexeme}>"
    }

    class Class(val identifier: Token, val superClass: Class?, methods: List<Function>) : KoxCallable {
        private val methodsMap: Map<String, Function> = methods.associateBy { it.name }
        private val initializer get() = findMethod("init")

        override val arity: Int get() = initializer?.arity ?: super.arity

        fun findMethod(name: String): Function? = methodsMap[name]
            ?: superClass?.findMethod(name)

        override fun call(interpreter: Interpreter, arguments: List<Any?>): Instance {
            val instance = Instance(this)

            initializer?.bind(instance)?.call(interpreter, arguments)

            return instance
        }

        override fun toString(): String = identifier.lexeme
    }

    class Instance(private val klass: Class) {
        private val fields = HashMap<String, Any?>()

        fun get(identifier: Token): Any? {
            val name = identifier.lexeme
            if (fields.containsKey(name)) {
                return fields[name]
            }

            klass.findMethod(name)?.let { return it.bind(this) }

            throw Interpreter.RuntimeError(
                identifier,
                "Undefined property $name"
            )
        }

        fun set(identifier: Token, value: Any?) {
            fields[identifier.lexeme] = value
        }

        override fun toString(): String = klass.identifier.lexeme + ".instance"
    }
}
