package io.lexcao.kox.common

sealed interface Stmt : Grammar {

    interface Visitor<R> {
        fun visit(stmt: Block): R
        fun visit(stmt: Expression): R
        fun visit(stmt: Print): R
        fun visit(stmt: Var): R
        fun visit(stmt: Fun): R
        fun visit(stmt: If): R
        fun visit(stmt: While): R
        fun visit(stmt: Return): R
        fun visit(stmt: Class): R
    }

    fun <R> accept(visitor: Visitor<R>): R

    data class Block(val statements: List<Stmt>) : Stmt {
        constructor(vararg statement: Stmt) : this(statement.toList())

        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Expression(val expr: Expr) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Print(val expr: Expr) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Var(val identifier: Token, val initializer: Expr?) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Fun(val identifier: Token, val params: List<Token>, val body: Block) : Stmt {
        val initializer: Boolean = identifier.lexeme == "init"
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Class(val identifier: Token, val superClass: Expr.Variable?, val methods: List<Fun>) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class While(val condition: Expr, val body: Stmt) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class For(val initializer: Stmt?, val condition: Expr?, val increment: Expr?, val body: Stmt) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R {
            throw IllegalAccessError("de-sugared by while")
        }
    }

    data class Return(val keyword: Token, val expr: Expr?) : Stmt {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }
}
