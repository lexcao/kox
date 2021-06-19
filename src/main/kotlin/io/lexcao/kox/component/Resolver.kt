package io.lexcao.kox.component

import io.lexcao.kox.Kox
import io.lexcao.kox.common.ClassType
import io.lexcao.kox.common.Expr
import io.lexcao.kox.common.FunctionType
import io.lexcao.kox.common.Stmt
import io.lexcao.kox.common.Token
import java.util.LinkedList

class Resolver(
    private val interpreter: Interpreter
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private val scopes = LinkedList<MutableMap<String, Boolean>>()
    private val scope get() = if (scopes.isEmpty()) null else scopes.peek()
    private var currentFunction = FunctionType.None
    private var currentClass = ClassType.None

    private val Token.initializing get() = scope?.containsKey(lexeme) == true
    private var Token.initialized
        get() = scope?.get(lexeme) == true
        set(value) {
            scope?.put(lexeme, value)
        }

    override fun visit(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visit(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visit(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.callee)
    }

    override fun visit(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visit(expr: Expr.Literal) {
        // do nothing
    }

    override fun visit(expr: Expr.This) {
        if (currentClass == ClassType.None) {
            Kox.error(expr.keyword, "Can't use 'this' outside of a class")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visit(expr: Expr.Super) {
        if (currentClass == ClassType.None) {
            Kox.error(expr.keyword, "Can't use 'super' outside of a class")
            return
        }
        if (currentClass != ClassType.Subclass) {
            Kox.error(expr.keyword, "Can't use 'super' with no superclass")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visit(expr: Expr.Unary) {
        resolve(expr.right)
    }

    override fun visit(expr: Expr.Call) {
        resolve(expr.callee)
        expr.arguments.forEach(this::resolve)
    }

    override fun visit(expr: Expr.Get) {
        resolve(expr.callee)
    }

    override fun visit(expr: Expr.Variable) {
        if (expr.identifier.initializing && !expr.identifier.initialized) {
            Kox.error(
                expr.identifier,
                "Can't read local variable in its own initializer"
            )
        }
        resolveLocal(expr, expr.identifier)
    }

    private fun resolveLocal(expr: Expr, identifier: Token) {
        scopes
            .withIndex()
            .firstOrNull { it.value.containsKey(identifier.lexeme) }
            ?.let { interpreter.resolve(expr, it.index) }
    }


    override fun visit(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.identifier)
    }

    override fun visit(stmt: Stmt.Block) {
        withScope {
            resolve(stmt.statements)
        }
    }

    fun resolve(statements: List<Stmt>) {
        statements.forEach(this::resolve)
    }

    private fun resolve(stmt: Stmt) = stmt.accept(this)
    private fun resolve(expr: Expr) = expr.accept(this)

    private inline fun withScope(block: () -> Unit) {
        try {
            beginScope()
            block()
        } finally {
            endScope()
        }
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }

    override fun visit(stmt: Stmt.Expression) {
        resolve(stmt.expr)
    }

    override fun visit(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visit(stmt: Stmt.Var) {
        declare(stmt.identifier)
        stmt.initializer?.apply(this::resolve)
        define(stmt.identifier)
    }

    private fun declare(identifier: Token) {
        if (identifier.initializing) {
            Kox.error(identifier, "Already defined variable with this name in this scope")
        }
        identifier.initialized = false
    }

    private fun define(identifier: Token) {
        identifier.initialized = true
    }

    override fun visit(stmt: Stmt.Fun) {
        declare(stmt.identifier)
        define(stmt.identifier)
        resolveFunction(stmt, FunctionType.Function)
    }

    override fun visit(stmt: Stmt.Class) {
        withClass(ClassType.Class) {

            declare(stmt.identifier)
            define(stmt.identifier)

            val className = stmt.identifier.lexeme
            val superClassName = stmt.superClass?.identifier?.lexeme
            if (className == superClassName) {
                Kox.error(
                    stmt.superClass.identifier,
                    "A class can't inherit from itself"
                )
            }

            stmt.superClass?.run {
                currentClass = ClassType.Subclass
                resolve(this)
                beginScope()
                scope!!["super"] = true
            }

            withScope {
                scope!!["this"] = true
                stmt.methods.forEach {
                    val declaration = if (it.initializer) FunctionType.Initializer else FunctionType.Method
                    resolveFunction(it, declaration)
                }
            }

            stmt.superClass?.run {
                endScope()
            }
        }
    }

    private fun withClass(type: ClassType, block: () -> Unit) {
        val enclosingClass = currentClass
        try {
            currentClass = type
            block()
        } finally {
            currentClass = enclosingClass
        }
    }

    private fun resolveFunction(function: Stmt.Fun, type: FunctionType) {
        withFunction(type) {
            withScope {
                function.params.forEach {
                    declare(it)
                    define(it)
                }
                resolve(function.body)
            }
        }
    }

    private inline fun withFunction(type: FunctionType, block: () -> Unit) {
        val enclosingFunction = currentFunction
        try {
            currentFunction = type
            block()
        } finally {
            currentFunction = enclosingFunction
        }
    }

    override fun visit(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.apply(this::resolve)
    }

    override fun visit(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visit(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.None) {
            Kox.error(stmt.keyword, "Can't return from top-level code")
            return
        }
        stmt.expr?.let {
            if (currentFunction == FunctionType.Initializer) {
                Kox.error(stmt.keyword, "Can't return value from an initializer")
            }
            resolve(it)
        }
    }
}
