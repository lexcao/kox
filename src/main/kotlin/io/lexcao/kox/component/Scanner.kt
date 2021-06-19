package io.lexcao.kox.component

import io.lexcao.kox.Kox
import io.lexcao.kox.common.Token
import io.lexcao.kox.common.TokenType
import io.lexcao.kox.common.TokenType.Bang
import io.lexcao.kox.common.TokenType.BangEqual
import io.lexcao.kox.common.TokenType.Comma
import io.lexcao.kox.common.TokenType.Dot
import io.lexcao.kox.common.TokenType.EOF
import io.lexcao.kox.common.TokenType.Equal
import io.lexcao.kox.common.TokenType.EqualEqual
import io.lexcao.kox.common.TokenType.Greater
import io.lexcao.kox.common.TokenType.GreaterEqual
import io.lexcao.kox.common.TokenType.Identifier
import io.lexcao.kox.common.TokenType.LeftBrace
import io.lexcao.kox.common.TokenType.LeftParen
import io.lexcao.kox.common.TokenType.Less
import io.lexcao.kox.common.TokenType.LessEqual
import io.lexcao.kox.common.TokenType.Minus
import io.lexcao.kox.common.TokenType.Plus
import io.lexcao.kox.common.TokenType.RightBrace
import io.lexcao.kox.common.TokenType.RightParen
import io.lexcao.kox.common.TokenType.Semicolon
import io.lexcao.kox.common.TokenType.Slash
import io.lexcao.kox.common.TokenType.Star
import java.util.Collections
import java.util.LinkedList

class Scanner(
    private val source: String
) {
    private val tokens = LinkedList<Token>()

    private var start = 0
    private var current = 0
    private var line = 1

    private val eof get() = current >= source.length
    private val text get() = source.substring(start, current)

    fun scanTokens(): List<Token> {
        while (!eof) {
            scanToken()
            start = current
        }

        addToken(EOF)
        return Collections.unmodifiableList(tokens)
    }

    private fun scanToken() {
        when (val c = advance()) {

            '(' -> addToken(LeftParen)
            ')' -> addToken(RightParen)
            '{' -> addToken(LeftBrace)
            '}' -> addToken(RightBrace)
            ',' -> addToken(Comma)
            '.' -> addToken(Dot)
            '-' -> addToken(Minus)
            '+' -> addToken(Plus)
            ';' -> addToken(Semicolon)
            '*' -> addToken(Star)

            '!' -> addToken(if (match('=')) BangEqual else Bang)
            '=' -> addToken(if (match('=')) EqualEqual else Equal)
            '<' -> addToken(if (match('=')) LessEqual else Less)
            '>' -> addToken(if (match('=')) GreaterEqual else Greater)

            '/' -> {
                if (match('/')) {
                    // comment until end of the line
                    while (peek() != '\n' && !eof) advance()
                } else {
                    addToken(Slash)
                }
            }

            ' ', '\r', '\t' -> {
                // ignore whitespace
            }

            '\n' -> line++

            '"' -> string()

            else -> {
                when {
                    c.isDigit() -> number()
                    c.isLetter() -> identifier()
                    else -> Kox.error(line, "Unexpected character [$c]")
                }
            }
        }
    }

    private fun identifier() {
        while (peek().isLetterOrDigit()) advance()

        addToken(TokenType.keyword(text) ?: Identifier)
    }

    private fun number() {
        while (peek().isDigit()) advance()

        // look for .
        if (peek() == '.' && peekNext().isDigit()) {
            advance()
            while (peek().isDigit()) advance()
        }

        addToken(TokenType.Number, text.toDouble())
    }

    private fun string() {
        while (peek() != '"' && !eof) {
            // multi-line strings support
            if (peek() == '\n') line++
            advance()
        }

        if (eof) {
            Kox.error(line, "Unterminated string")
            return
        }

        // closing "
        advance()

        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.String, value)
    }

    private fun peek() = if (eof) Char.MIN_VALUE else source[current]

    private fun peekNext(): Char {
        if (current + 1 >= source.length) return Char.MIN_VALUE
        return source[current + 1]
    }

    private fun advance() = source[current++]

    private fun match(expected: Char): Boolean {
        if (eof) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun addToken(type: TokenType, literal: Any? = null) {
        tokens += Token(line = line, literal = literal, lexeme = text, type = type)
    }
}