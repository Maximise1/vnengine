package com.maximise.vnengine.engine.lexer

class Lexer {

    private var cursor: Int = 0
    private var line: Int = 0
    private var col: Int = 0
    private var script: String = ""

    private fun peek(offset: Int = 0): Char? {
        return script.getOrNull(cursor + offset)
    }

    fun tokenize(s: String): List<Token> {
        script = s
        val tokens: MutableList<Token> = mutableListOf()

        while (peek() != null) {
            val token = parseToken()
            tokens.add(token)
        }

        return tokens
    }

    private fun parseToken(): Token {
        var next = peek()

        val token = when (next) {
            '"' -> parseString()
            else -> throw RuntimeException("Unexpected symbol encountered: $next")
        }

        return token
    }

    private fun parseString(): Token {
        // TODO
    }
}