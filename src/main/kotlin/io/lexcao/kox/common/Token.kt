package io.lexcao.kox.common

data class Token(
    val line: Int,
    val type: TokenType,
    val lexeme: String = "",
    val literal: Any? = null
) {
    override fun toString() = "$type $lexeme $literal"
}
