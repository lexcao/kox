package io.lexcao.kox.component

import io.lexcao.kox.common.Token
import io.lexcao.kox.common.TokenType
import io.lexcao.kox.common.TokenType.And
import io.lexcao.kox.common.TokenType.Bang
import io.lexcao.kox.common.TokenType.BangEqual
import io.lexcao.kox.common.TokenType.Class
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
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ScannerTest {

    @MethodSource
    @ParameterizedTest(name = "{index}: {0}")
    fun scanTokens(source: String, expect: Collection<Token>) {
        // WHEN
        val actual = Scanner(source).scanTokens()

        // THEN
        assertIterableEquals(expect, actual)
    }

    private fun scanTokens(): List<Arguments> {
        val singleChar = "(){},.-+;*/"
        val expectSingleCharTokens = listOf(
            token(LeftParen, "("),
            token(RightParen, ")"),
            token(LeftBrace, "{"),
            token(RightBrace, "}"),
            token(Comma, ","),
            token(Dot, "."),
            token(Minus, "-"),
            token(Plus, "+"),
            token(Semicolon, ";"),
            token(Star, "*"),
            token(Slash, "/"),
            token(EOF)
        )

        val oneOrTwoChar = "! != = == <> <= >="
        val expectOneOrTwoChar = listOf(
            token(Bang, "!"),
            token(BangEqual, "!="),
            token(Equal, "="),
            token(EqualEqual, "=="),
            token(Less, "<"),
            token(Greater, ">"),
            token(LessEqual, "<="),
            token(GreaterEqual, ">="),
            token(EOF)
        )

        val string = "\"this is string\""
        val expectString = listOf(
            token(TokenType.String, "\"this is string\"", "this is string"),
            token(EOF)
        )

        val number = "123 1.23"
        val expectNumber = listOf(
            token(TokenType.Number, "123", 123.0),
            token(TokenType.Number, "1.23", 1.23),
            token(EOF)
        )

        val identifier = "abc"
        val expectIdentifier = listOf(
            token(Identifier, "abc"),
            token(EOF)
        )

        val keyword = "and class else false for fun if nil or print return super this true var while"
        val expectKeyword = listOf(
            token(And, "and"),
            token(Class, "class"),
            token(Else, "else"),
            token(False, "false"),
            token(For, "for"),
            token(Fun, "fun"),
            token(If, "if"),
            token(Nil, "nil"),
            token(Or, "or"),
            token(Print, "print"),
            token(Return, "return"),
            token(Super, "super"),
            token(This, "this"),
            token(True, "true"),
            token(Var, "var"),
            token(While, "while"),
            token(EOF)
        )

        return listOf(
            arguments(singleChar, expectSingleCharTokens),
            arguments(oneOrTwoChar, expectOneOrTwoChar),
            arguments(string, expectString),
            arguments(number, expectNumber),
            arguments(identifier, expectIdentifier),
            arguments(keyword, expectKeyword),
        )
    }

    private fun token(type: TokenType, lexeme: String = "", literal: Any? = null, line: Int = 1) = Token(
        type = type,
        line = line,
        lexeme = lexeme,
        literal = literal
    )
}
