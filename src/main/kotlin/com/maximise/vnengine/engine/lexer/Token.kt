package com.maximise.vnengine.engine.lexer

sealed class Token(val line: Int, val col: Int) {
    data class StringLiteral(val value: String, val l: Int, val c: Int) : Token(l, c)
}