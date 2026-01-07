package com.maximise.vnengine.engine.lexer

sealed class Token(val line: Int, val col: Int) {
    data class StringLiteral(val value: String, val l: Int, val c: Int) : Token(l, c)
    data class NumberLiteral(val value: Double, val l: Int, val c: Int) : Token(l, c)
    data class OpenBracket(val l: Int, val c: Int) : Token(l, c)
    data class CloseBracket(val l: Int, val c: Int) : Token(l, c)
    data class Keyword(val value: String, val c: Int, val l: Int) : Token(l, c)
    data class Identifier(val value: String, val c: Int, val l: Int): Token(l, c)
    object EOF : Token(-1, -1)
}

val KEYWORDS = listOf("block", "execute")