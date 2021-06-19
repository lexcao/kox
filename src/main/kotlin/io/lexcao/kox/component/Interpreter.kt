package io.lexcao.kox.component

import io.lexcao.kox.Kox
import io.lexcao.kox.common.Expr
import io.lexcao.kox.common.KoxCallable
import io.lexcao.kox.common.Stmt
import io.lexcao.kox.common.Token
import io.lexcao.kox.common.TokenType
import io.lexcao.kox.common.TokenType.Bang
import io.lexcao.kox.common.TokenType.BangEqual
import io.lexcao.kox.common.TokenType.EqualEqual
import io.lexcao.kox.common.TokenType.Greater
import io.lexcao.kox.common.TokenType.GreaterEqual
import io.lexcao.kox.common.TokenType.Less
import io.lexcao.kox.common.TokenType.LessEqual
import io.lexcao.kox.common.TokenType.Minus
import io.lexcao.kox.common.TokenType.Or
import io.lexcao.kox.common.TokenType.Plus
import io.lexcao.kox.common.TokenType.Slash
import io.lexcao.kox.common.TokenType.Star

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    val globals = Environment()
    private val locals = HashMap<Expr, Int>()
    private var environment = globals

    init {
        registerNatives()
    }

    private fun registerNatives() {
        globals.define("clock", object : KoxCallable {
            override fun call(
                interpreter: Interpreter,
                arguments: List<Any?>
            ): Double = System.currentTimeMillis() / 1000.0

            override fun toString(): String = "<native fn clock>"
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach(this::execute)
        } catch (error: RuntimeError) {
            Kox.runtimeError(error)
        }
    }

    override fun visit(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        when (val tokenType = expr.operator.type) {
            Plus -> {
                if (left is Double && right is Double) {
                    return left + right
                }

                if (left is String) {
                    return left + right
                }

                throw RuntimeError(expr.operator, "Operands must be two numbers or two strings")
            }

            in TokenType.binary() -> {
                val (a, b) = checkNumberOperand(expr.operator, left, right)
                return when (tokenType) {
                    Minus -> a - b
                    Slash -> a / b
                    Star -> a * b
                    Plus -> a + b

                    Greater -> a > b
                    GreaterEqual -> a >= b
                    Less -> a < b
                    LessEqual -> a <= b

                    else -> Unit
                }
            }


            BangEqual -> return !isEqual(left, right)
            EqualEqual -> return isEqual(left, right)
            else -> {
            }
        }

        error("unreachable")
    }

    override fun visit(expr: Expr.Logical): Any? {
        val left = evaluate(expr.left)

        // short-circuit
        if (expr.operator.type == Or) {
            if (isTruthy(left)) return left
        } else {
            if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visit(expr: Expr.Set): Any? {
        val callee = evaluate(expr.callee) as? KoxCallable.Instance
            ?: throw RuntimeError(expr.identifier, "Only instances have fields")

        val value = evaluate(expr.value)

        callee.set(expr.identifier, value)

        return value
    }

    private fun isEqual(a: Any?, b: Any?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return a == b
    }

    override fun visit(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    override fun visit(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visit(expr: Expr.This): Any? {
        return lookupVariable(expr.keyword, expr)
    }

    override fun visit(expr: Expr.Super): Any {
        val distance = locals[expr]!!
        val superClass = environment.getAt(distance, "super") as KoxCallable.Class
        val instance = environment.getAt(distance - 1, "this") as KoxCallable.Instance
        val method = superClass.findMethod(expr.method.lexeme)
            ?: throw RuntimeError(expr.method, "Undefined property '${expr.method.lexeme}'")

        return method.bind(instance)
    }

    override fun visit(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            Bang -> !isTruthy(right)
            Minus -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }
            else -> error("Unreachable")
        }
    }

    override fun visit(expr: Expr.Call): Any? {
        val callable = evaluate(expr.callee) as? KoxCallable
            ?: throw RuntimeError(expr.paren, "Can only call functions and classes")

        if (expr.arguments.size != callable.arity) {
            throw RuntimeError(
                expr.paren,
                "Expected ${callable.arity} arguments but got ${expr.arguments.size}"
            )
        }

        val arguments = expr.arguments.map(this::evaluate)

        return callable.call(this, arguments)
    }

    override fun visit(expr: Expr.Get): Any? {
        val callee = evaluate(expr.callee) as? KoxCallable.Instance
            ?: throw RuntimeError(expr.identifier, "Only instances have properties")

        return callee.get(expr.identifier)
    }

    override fun visit(expr: Expr.Variable): Any? {
        return lookupVariable(expr.identifier, expr)
    }

    private fun lookupVariable(identifier: Token, expr: Expr) = locals[expr]
        ?.let { environment.getAt(it, identifier) }
        ?: globals.get(identifier)

    override fun visit(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        assignVariable(expr, value)

        return value
    }

    private fun assignVariable(variable: Expr.Assign, value: Any?) {
        locals[variable]?.let {
            environment.assignAt(it, variable.identifier, value)
        } ?: globals.assign(variable.identifier, value)
    }

    override fun visit(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visit(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visit(stmt: Stmt.Var) {
        val value = stmt.initializer?.let(this::evaluate)
        environment.define(stmt.identifier, value)
    }

    override fun visit(stmt: Stmt.Fun) {
        val function = KoxCallable.Function(stmt, environment)
        environment.define(stmt.identifier, function)
    }

    override fun visit(stmt: Stmt.Class) {
        val superClass = stmt.superClass?.let {
            evaluate(it) as? KoxCallable.Class
                ?: throw RuntimeError(stmt.superClass.identifier, "Superclass must be a class")
        }

        environment.define(stmt.identifier, null)

        superClass?.run {
            environment = Environment(environment)
            environment.define("super", this)
        }

        val methods = stmt.methods.map {
            KoxCallable.Function(it, environment, it.initializer)
        }

        val klass = KoxCallable.Class(stmt.identifier, superClass, methods)

        superClass?.run {
            environment = environment.enclosing!!
        }

        environment.assign(stmt.identifier, klass)
    }

    override fun visit(stmt: Stmt.Block) {
        executeBlock(stmt, Environment(environment))
    }

    override fun visit(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else {
            stmt.elseBranch?.let(this::execute)
        }
    }

    override fun visit(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visit(stmt: Stmt.Return) {
        val value = stmt.expr?.let(this::evaluate)
        throw Return(value)
    }

    fun executeBlock(block: Stmt.Block, environment: Environment) {
        withEnvironment(environment) {
            block.statements.forEach(this::execute)
        }
    }

    private inline fun withEnvironment(env: Environment, block: () -> Unit) {
        val previous = environment
        try {
            environment = env
            block()
        } finally {
            environment = previous
        }
    }

    private fun checkNumberOperand(operator: Token, vararg operands: Any?): Array<Double> {
        val numbers = operands.filterIsInstance<Double>()
        if (numbers.size != operands.size) {
            throw RuntimeError(operator, "Operand(s) must be a number")
        }
        return numbers.toTypedArray()
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    private fun execute(stmt: Stmt) = stmt.accept(this)

    private fun stringify(value: Any?): String {
        value ?: return "nil"

        if (value is Double) {
            val text = value.toString()
            if (text.endsWith(".0")) {
                return text.dropLast(2)
            }
        }

        return value.toString()
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    class RuntimeError(val token: Token, message: String) : RuntimeException(message)
    class Return(val value: Any?) : RuntimeException(null, null, false, false)
}