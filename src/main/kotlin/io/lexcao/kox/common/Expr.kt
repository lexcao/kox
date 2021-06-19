package io.lexcao.kox.common

sealed interface Expr : Grammar {

    interface Visitor<R> {
        fun visit(expr: Binary): R
        fun visit(expr: Logical): R
        fun visit(expr: Set): R
        fun visit(expr: This): R
        fun visit(expr: Super): R
        fun visit(expr: Grouping): R
        fun visit(expr: Literal): R
        fun visit(expr: Unary): R
        fun visit(expr: Call): R
        fun visit(expr: Get): R
        fun visit(expr: Variable): R
        fun visit(expr: Assign): R
    }

    fun <R> accept(visitor: Visitor<R>): R

    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Set(val callee: Expr, val identifier: Token, val value: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class This(val keyword: Token) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Super(val keyword: Token, val method: Token) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Grouping(val expression: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Literal(val value: Any?) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Unary(val operator: Token, val right: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Get(val callee: Expr, val identifier: Token) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Variable(val identifier: Token) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }

    data class Assign(val identifier: Token, val value: Expr) : Expr {
        override fun <R> accept(visitor: Visitor<R>): R = visitor.visit(this)
    }
}
