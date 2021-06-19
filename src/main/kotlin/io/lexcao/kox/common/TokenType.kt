package io.lexcao.kox.common

enum class TokenType {
    // Single character tokens
    LeftParen, RightParen, LeftBrace, RightBrace,
    Comma, Dot, Minus, Plus, Semicolon, Slash, Star,

    // One or two character tokens
    Bang, BangEqual,
    Equal, EqualEqual,
    Greater, GreaterEqual,
    Less, LessEqual,

    // Literals
    Identifier, String, Number,

    // Keywords
    And, Class, Else, False, Fun, For, If, Nil, Or,
    Print, Return, Super, This, True, Var, While,

    EOF;

    companion object {

        private val keywords = keywords().associateBy { it.name.lowercase() }

        fun keywords() = listOf(
            And, Class, Else, False, Fun, For, If, Nil, Or,
            Print, Return, Super, This, True, Var, While,
        )

        fun keyword(name: kotlin.String) = keywords[name]

        fun binary() = listOf(
            Plus, Minus, Slash, Star,
            Greater, GreaterEqual, Less, LessEqual
        )
    }
}
