package io.lexcao.kox.component

import io.lexcao.kox.common.Token


class Environment(val enclosing: Environment? = null) {
    private val values = LinkedHashMap<String, Any?>()

    fun define(identifier: Token, value: Any?) {
        define(identifier.lexeme, value)
    }

    fun define(identifier: String, value: Any?) {
        values[identifier] = value
    }

    fun assign(identifier: Token, value: Any?) {
        val name = identifier.lexeme
        if (values.containsKey(name)) {
            values[name] = value
            return
        }

        enclosing?.assign(identifier, value)
            ?: throw Interpreter.RuntimeError(
                identifier,
                "Undefined variable '$name'"
            )
    }

    fun get(identifier: Token): Any? {
        val name = identifier.lexeme
        if (values.containsKey(name)) {
            return values[name]
        }

        return enclosing?.get(identifier)
            ?: throw Interpreter.RuntimeError(
                identifier,
                "Undefined variable '$name'"
            )
    }

    fun getAt(distance: Int, identifier: Token) = getAt(distance, identifier.lexeme)
    fun getAt(distance: Int, identifier: String) = ancestor(distance).values[identifier]

    private fun ancestor(distance: Int): Environment {
        var environment = this
        var i = distance
        while (i-- > 0) {
            environment = environment.enclosing!!
        }
        return environment
    }

    fun assignAt(distance: Int, identifier: Token, value: Any?) {
        ancestor(distance).values[identifier.lexeme] = value
    }
}
