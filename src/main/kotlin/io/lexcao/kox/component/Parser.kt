package io.lexcao.kox.component

import io.lexcao.kox.Kox
import io.lexcao.kox.common.Expr
import io.lexcao.kox.common.Stmt
import io.lexcao.kox.common.Token
import io.lexcao.kox.common.TokenType
import io.lexcao.kox.common.TokenType.And
import io.lexcao.kox.common.TokenType.Bang
import io.lexcao.kox.common.TokenType.BangEqual
import io.lexcao.kox.common.TokenType.Comma
import io.lexcao.kox.common.TokenType.Dot
import io.lexcao.kox.common.TokenType.EOF
import io.lexcao.kox.common.TokenType.Else
import io.lexcao.kox.common.TokenType.Equal
import io.lexcao.kox.common.TokenType.EqualEqual
import io.lexcao.kox.common.TokenType.False
import io.lexcao.kox.common.TokenType.For
import io.lexcao.kox.common.TokenType.Fun
import io.lexcao.kox.common.TokenType.Greater
import io.lexcao.kox.common.TokenType.GreaterEqual
import io.lexcao.kox.common.TokenType.Identifier
import io.lexcao.kox.common.TokenType.If
import io.lexcao.kox.common.TokenType.LeftBrace
import io.lexcao.kox.common.TokenType.LeftParen
import io.lexcao.kox.common.TokenType.Less
import io.lexcao.kox.common.TokenType.LessEqual
import io.lexcao.kox.common.TokenType.Minus
import io.lexcao.kox.common.TokenType.Nil
import io.lexcao.kox.common.TokenType.Or
import io.lexcao.kox.common.TokenType.Plus
import io.lexcao.kox.common.TokenType.Print
import io.lexcao.kox.common.TokenType.Return
import io.lexcao.kox.common.TokenType.RightBrace
import io.lexcao.kox.common.TokenType.RightParen
import io.lexcao.kox.common.TokenType.Semicolon
import io.lexcao.kox.common.TokenType.Slash
import io.lexcao.kox.common.TokenType.Star
import io.lexcao.kox.common.TokenType.Super
import io.lexcao.kox.common.TokenType.This
import io.lexcao.kox.common.TokenType.True
import io.lexcao.kox.common.TokenType.Var
import io.lexcao.kox.common.TokenType.While

class Parser(private val tokens: List<Token>) {
    private var current = 0
    private val eof get() = peek().type == EOF

    fun parse(): List<Stmt> = sequence {
        while (!eof) {
            yield(declaration())
        }
    }.toList()

    /**
     * declaration -> classDecl
     *              | funDecl
     *              | varDecl
     *              | statement ;
     */
    private fun declaration(): Stmt {
        try {
            return when {
                match(TokenType.Class) -> classDecl()
                match(Fun) -> funDecl()
                match(Var) -> varDecl()
                else -> statement()
            }
        } catch (e: ParseError) {
            synchronize()
            error("unreachable")
        }
    }

    /**
     * classDecl   -> "class" Identifier ( "<" Identifier )?
     *                "{" function* "}" ;
     */
    private fun classDecl(): Stmt {
        val identifier = consume(Identifier, "Expect class name")

        val superClass = if (match(Less)) {
            consume(Identifier, "Expect superclass name")
            Expr.Variable(previous())
        } else null

        consume(LeftBrace, "Expect '{' before class body")

        val methods = ArrayList<Stmt.Fun>()
        while (!check(RightBrace) && !eof) {
            methods += function("method")
        }

        consume(RightBrace, "Expect '}' after class body")
        return Stmt.Class(identifier, superClass, methods)
    }

    /**
     * funDecl     -> "fun" function;
     */
    private fun funDecl(): Stmt {
        return function("function")
    }

    /**
     * function    -> Identifier "(" parameters? ")" block ;
     */
    private fun function(kind: String): Stmt.Fun {
        val identifier = consume(Identifier, "Expect $kind name")
        consume(LeftParen, "Expect '(' after function name")

        val params = parameters()

        consume(RightParen, "Expect ')' after parameters")

        consume(LeftBrace, "Expect '{' before $kind body")
        val body = block()

        return Stmt.Fun(identifier, params, body)
    }

    /**
     * parameters  -> Identifier ( "," Identifier )* ;
     */
    private fun parameters(): ArrayList<Token> {
        val params = ArrayList<Token>()
        if (!check(RightParen)) {
            do {
                if (params.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters")
                }
                params += consume(Identifier, "Expect parameter name")
            } while (match(Comma))
        }
        return params
    }

    /**
     * varDecl    -> "var" Identifier ( "=" expression )? ";" ;
     */
    private fun varDecl(): Stmt {
        val identifier = consume(Identifier, "Expect variable name")

        var initializer: Expr? = null
        if (match(Equal)) {
            initializer = expression()
        }

        consumeSemicolon("variable declaration")
        return Stmt.Var(identifier, initializer)
    }

    /**
     * statement   -> exprStmt
     *              | whileStmt
     *              | forStmt
     *              | returnStmt
     *              | ifStmt
     *              | printStmt
     *              | block ;
     */
    private fun statement(): Stmt = when {
        match(Print) -> printStmt()
        match(LeftBrace) -> block()
        match(If) -> ifStmt()
        match(While) -> whileStmt()
        match(For) -> forStmt()
        match(Return) -> returnStmt()
        else -> exprStmt()
    }

    private fun returnStmt(): Stmt {
        val keyword = previous()
        val expr = if (check(Semicolon)) null else expression()

        consumeSemicolon("return value")
        return Stmt.Return(keyword, expr)
    }

    /**
     * forStmt     -> "for" "(" ( varDecl | exprStmt | ";" )
     *                 expression? ";"
     *                 expression? ")" statement ;
     */
    private fun forStmt(): Stmt {
        consume(LeftParen, "Expect '(' after 'for'")

        val initializer = when {
            match(Var) -> varDecl()
            match(Semicolon) -> null
            else -> exprStmt()
        }

        val condition = if (check(Semicolon)) null else expression()
        consumeSemicolon("for")

        val increment = if (check(RightParen)) null else expression()
        consume(RightParen, "Expect ')' after 'for'")

        val body = statement()

        return deSugar(Stmt.For(initializer, condition, increment, body))
    }

    /**
     * de-sugar for to while
     * for (a, b, c) {d} ->
     * a while(b) {d c}
     */
    private fun deSugar(stmt: Stmt.For): Stmt {
        val body = stmt.increment?.let {
            Stmt.Block(stmt.body, Stmt.Expression(it))
        } ?: stmt.body

        val condition = stmt.condition ?: Expr.Literal(true)

        val whileStmt = Stmt.While(condition, body)

        return stmt.initializer?.let {
            Stmt.Block(it, whileStmt)
        } ?: whileStmt
    }

    /**
     * whileStmt   -> "while" "(" expression ")" statement ;
     */
    private fun whileStmt(): Stmt {
        consume(LeftParen, "Expect '(' after 'while'")
        val condition = expression()
        consume(RightParen, "Expect ')' after 'while'")
        val body = statement()
        return Stmt.While(condition, body)
    }

    /**
     * ifStmt      -> "if" "(" expression ")" statement
     *                ( "else" statement )? ;
     */
    private fun ifStmt(): Stmt {
        consume(LeftParen, "Expect '(' after 'if'")
        val condition = expression()
        consume(RightParen, "Expect ')' after 'if'")
        val thenBranch = statement()
        val elseBranch = if (match(Else)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    /**
     * block       -> "{" declaration* "}" ;
     */
    private fun block(): Stmt.Block {
        val statements = ArrayList<Stmt>()
        while (!check(RightBrace) && !eof) {
            statements += declaration()
        }
        consume(RightBrace, "Expect '}' after block")
        return Stmt.Block(statements)
    }

    /**
     * exprStmt    -> expression ";" ;
     */
    private fun exprStmt(): Stmt {
        val expr = expression()
        consumeSemicolon("expression")
        return Stmt.Expression(expr)
    }

    /**
     * printStmt   -> "print" expression ";" ;
     */
    private fun printStmt(): Stmt {
        val expr = expression()
        consumeSemicolon("value")
        return Stmt.Print(expr)
    }

    private fun synchronize() {
        advance()

        while (!eof) {
            if (previous().type == Semicolon) return

            when (peek().type) {
                TokenType.Class, Fun, Var,
                For, If, While, Print, Return -> return
                else -> {

                }
            }
        }

        advance()
    }

    /**
     * expression -> assignment ;
     */
    private fun expression(): Expr {
        return assignment()
    }

    /**
     * assignment  -> ( call "." )? Identifier "=" assignment
     *              | logic_or ;
     */
    private fun assignment(): Expr {
        val expr = or()

        if (match(Equal)) {
            val equals = previous()
            val value = assignment()

            when (expr) {
                is Expr.Variable -> {
                    return Expr.Assign(expr.identifier, value)
                }
                is Expr.Get -> {
                    return Expr.Set(expr.callee, expr.identifier, value)
                }
                else -> error(equals, "Invalid assignment target")
            }
        }

        return expr
    }

    /**
     * logic_or    -> logic_and ( "or" logic_and )* ;
     */
    private fun or(): Expr {
        return downOrLogical(::and, Or)
    }

    /**
     * logic_and   -> equality ( "and" equality )* ;
     */
    private fun and(): Expr {
        return downOrLogical(::equality, And)
    }

    private fun downOrLogical(operation: () -> Expr, vararg matches: TokenType): Expr {
        var expr = operation()
        while (match(*matches)) {
            val operator = previous()
            val right = operation()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    /**
     * equality   -> comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private fun equality(): Expr {
        return downOrBinary(::comparison, BangEqual, EqualEqual)
    }

    /**
     * comparison -> term ( ( ">" | ">=" | "<" | "<=") term )* ;
     */
    private fun comparison(): Expr {
        return downOrBinary(::term, Greater, GreaterEqual, Less, LessEqual)
    }

    /**
     * term       -> factor ( ( "-" | "+" ) factor )* ;
     */
    private fun term(): Expr {
        return downOrBinary(::factor, Minus, Plus)
    }

    /**
     * factor     -> unary ( ( "/" | "*" ) unary )* ;
     */
    private fun factor(): Expr {
        return downOrBinary(::unary, Slash, Star)
    }

    /**
     * unary       -> ( "-" | "!" ) unary | call ;
     */
    private fun unary(): Expr {
        if (match(Bang, Minus)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    /**
     * call        -> primary ( "(" arguments? ")" | "." Identifier )* ;
     */
    private fun call(): Expr {
        var expr = primary()

        loop@ while (true) {
            expr = when {
                match(LeftParen) -> {
                    finishCall(expr)
                }
                match(Dot) -> {
                    val identifier = consume(Identifier, "Expect property name after '.' call")
                    Expr.Get(expr, identifier)
                }
                else -> {
                    break@loop
                }
            }
        }
        return expr
    }


    private fun finishCall(callee: Expr): Expr {
        val arguments = arguments()

        val paren = consume(RightParen, "Expect ')' after arguments")

        return Expr.Call(callee, paren, arguments)
    }

    private fun arguments(): ArrayList<Expr> {
        val arguments = ArrayList<Expr>()

        if (!check(RightParen)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments")
                }
                arguments += expression()
            } while (match(Comma))
        }
        return arguments
    }

    /**
     * primary    -> Number | String | "true" | "false" | "nil"
     *             | "(" expression ")"
     *             | This ;
     *             | Identifier ;
     */
    private fun primary(): Expr = when {
        match(False) -> Expr.Literal(false)
        match(True) -> Expr.Literal(true)
        match(Nil) -> Expr.Literal(null)
        match(TokenType.Number, TokenType.String) -> Expr.Literal(previous().literal)
        match(LeftParen) -> {
            val expr = expression()
            consume(RightParen, "Expect ')' after expression")
            Expr.Grouping(expr)
        }
        match(This) -> Expr.This(previous())
        match(Super) -> {
            val keyword = previous()
            consume(Dot, "Expect '.' after 'super'")
            val method = consume(Identifier, "Expect superclass method name")
            Expr.Super(keyword, method)
        }
        match(Identifier) -> Expr.Variable(previous())
        else -> throw error(peek(), "Expect expression")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw error(peek(), message)
    }

    private fun consumeSemicolon(message: String): Token {
        return consume(Semicolon, "Expect ';' at $message")
    }

    private fun error(token: Token, message: String): ParseError {
        if (token.type == EOF) {
            Kox.error(token.line, message, "at end")
        } else {
            Kox.error(token.line, message, "at '${token.lexeme}'")
        }
        return ParseError
    }

    private fun downOrBinary(operation: () -> Expr, vararg matches: TokenType): Expr {
        var expr = operation()
        while (match(*matches)) {
            val operator = previous()
            val right = operation()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun match(vararg token: TokenType): Boolean {
        return token.any { check(it) }.also {
            if (it) advance()
        }
    }

    private fun check(type: TokenType): Boolean {
        if (eof) return false
        return peek().type == type
    }

    private fun peek() = tokens[current]
    private fun advance(): Token {
        if (!eof) current++
        return previous()
    }

    private fun previous() = tokens[current - 1]

    object ParseError : RuntimeException()
}
