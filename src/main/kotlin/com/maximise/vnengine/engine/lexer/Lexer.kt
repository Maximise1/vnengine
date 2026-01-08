package com.maximise.vnengine.engine.lexer

class Lexer {

    private var cursor: Int = 0
    private var line: Int = 1
    private var col: Int = 1
    private var script: String = ""

    private fun peek(offset: Int = 0): Char? {
        return script.getOrNull(cursor + offset)
    }

    private fun advance(): Char? {
        val symbol = peek()

        symbol.let { it ->
            if (it == '\n') {
                col = 1
                line += 1
            } else {
                col += 1
            }
            cursor += 1
        }

        return symbol
    }

    fun tokenize(s: String): List<Token> {
        script = s
        val tokens: MutableList<Token> = mutableListOf()
        cursor = 0
        line = 1
        col = 1

        while (peek() != null) {
            val token = parseToken()
            //println(token)
            tokens.add(token)
        }

        return tokens
    }

    private fun parseToken(): Token {
        var next = peek()

        // Skipping newlines, comments, and whitespaces
        while (next == '/' || next == '\n' || next == ' ') {
            when (next) {
                ' ' -> advance()
                '/' -> if (peek(1) == '/') {
                    while ((peek() != '\n') && (peek() != null)) {
                        advance()!!.code
                    }
                    advance()
                } else {
                    throw RuntimeException("Unexpected symbol encountered: $next")
                }
                '\n' -> advance()
            }
            next = peek()
        }

        if (next == '"') {
            return parseString()
        }

        // end of file
        if (next == null) {
            return Token.EOF
        }

        // Parsing numbers
        if ((next.code < 58) && (next.code > 47)) {
            return parseNumber()
        }

        if (next == '{') {
            advance()
            return Token.OpenBracket(l = line, c = col)
        }

        if (next == '}') {
            advance()
            return Token.CloseBracket(l = line, c = col)
        }

        // Parsing keywords and identifiers
        if ((next == '_') || ((next.code > 66)
                    && (next.code < 91)) || (next.code > 96) && (next.code < 123)) {
            return parseLetters()
        }

        throw RuntimeException("Unexpected symbol encountered: $next")
    }

    private fun parseString(): Token {
        val value = StringBuilder()
        val startLine = line
        val startCol = col

        if (peek(1) == '"') {
            advance()
            advance()
            while ((peek() != '"') || (peek(1) != '"')) {
                val symbol = advance()
                if (symbol == null) {
                    throw RuntimeException("String Literal not properly enclosed")
                }
                value.append(symbol)
            }
            advance()
            advance()
        } else {
            advance()
            while ((peek() != '"')) {
                val symbol = advance()
                if (symbol == null) {
                    throw RuntimeException("String Literal not properly enclosed")
                }
                value.append(symbol)
            }
            advance()
        }

        return Token.StringLiteral(value = value.toString(), l = startLine, c = startCol)
    }
    private fun parseNumber(): Token {
        val value = StringBuilder()
        val startLine = line
        val startCol = col

        while (peek() != null && peek()!!.code < 58 && peek()!!.code > 47) {
            value.append(advance())
        }

        if (peek() == '.') {
            advance()
            while (peek() != null && peek()!!.code < 58 && peek()!!.code > 47) {
                value.append(advance())
            }
        }

        return Token.NumberLiteral(value = value.toString().toDouble(), l = startLine, c = startCol)
    }

    private fun parseLetters(): Token {
        var next = peek()
        var code = next?.code ?: 0
        val value = StringBuilder()
        val startLine = line
        val startCol = col

        while ((next == '_') || ((code > 66)
                    && (code < 91)) || (code > 96) && (code < 123)) {
            value.append(advance())
            next = peek()
            code = next?.code ?: 0
        }

        return if (value.toString() in KEYWORDS) {
            Token.Keyword(value = value.toString(), c = startCol, l = startLine)
        } else {
            Token.Identifier(value = value.toString(), c = startCol, l = startLine)
        }
    }
}