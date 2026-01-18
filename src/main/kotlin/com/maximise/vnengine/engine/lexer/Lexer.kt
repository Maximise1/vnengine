package com.maximise.vnengine.engine.lexer

class Lexer {

    private var cursor: Int = 0
    private var line: Int = 1
    private var col: Int = 1
    private var script: String = ""

    private val tokenMapper: Map<Char?, () -> Token> = mapOf(
        null to ::parseEOF,
        '"' to ::parseString,
        '{' to ::parseOpenBraces,
        '}' to ::parseCloseBraces,
        '(' to ::parseOpenParenthesis,
        ')' to ::parseCloseParenthesis,
        '=' to ::parseEqualOperators,
        '>' to ::parseGreaterOperators,
        '<' to ::parseLessOperators,
        '-' to ::parseMinusOperator,
        '+' to ::parsePlusOperator,
        '/' to ::parseDivOperator,
        '%' to ::parseRemOperator,
        '*' to ::parseMulAndPowOperators,
        '|' to ::parseOrOperator,
        '&' to ::parseAndOperator,
        '!' to ::parseNotOperator
    )

    private fun peek(offset: Int = 0): Char? {
        return script.getOrNull(cursor + offset)
    }

    private fun advance(): Char? {
        val symbol = peek()
        //println(symbol)

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

    private fun parseEOF(): Token {
        return Token.EOF
    }

    private fun parseOpenBraces(): Token {
        val token = Token.OpenBraces(l = line, c = col)
        advance()
        return token
    }

    private fun parseCloseBraces(): Token {
        val token = Token.CloseBraces(l = line, c = col)
        advance()
        return token
    }

    private fun parseOpenParenthesis(): Token {
        val token = Token.OpenParenthesis(l = line, c = col)
        advance()
        return token
    }

    private fun parseCloseParenthesis(): Token {
        val token = Token.CloseParenthesis(l = line, c = col)
        advance()
        return token
    }

    private fun parseEqualOperators(): Token {
        if (peek(1) == '=') {
            val token = Token.EqualsOperator(l = line, c = col)
            advance()
            advance()
            return token
        } else {
            val token = Token.AssignOperator(l = line, c = col)
            advance()
            return token
        }
    }

    private fun parseGreaterOperators(): Token {
        if (peek(1) == '=') {
            val token = Token.GreaterOrEqualOperator(l = line, c = col)
            advance()
            advance()
            return token
        } else {
            val token = Token.GreaterOperator(l = line, c = col)
            advance()
            return token
        }
    }

    private fun parseLessOperators(): Token {
        if (peek(1) == '=') {
            val token = Token.LessOrEqualOperator(l = line, c = col)
            advance()
            advance()
            return token
        } else {
            val token = Token.LessOperator(l = line, c = col)
            advance()
            return token
        }
    }

    private fun parseMinusOperator(): Token {
        val token = Token.MinusOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parsePlusOperator(): Token {
        val token = Token.PlusOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parseDivOperator(): Token {
        val token = Token.DivOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parseRemOperator(): Token {
        val token = Token.RemOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parseMulAndPowOperators(): Token {
        if (peek(1) == '*') {
            val token = Token.PowOperator(l = line, c = col)
            advance()
            advance()
            return token
        } else {
            val token = Token.MulOperator(l = line, c = col)
            advance()
            return token
        }
    }

    private fun parseOrOperator(): Token {
        val token = Token.OrOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parseAndOperator(): Token {
        val token = Token.AndOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parseNotOperator(): Token {
        val token = Token.NotOperator(l = line, c = col)
        advance()
        return token
    }

    private fun parseToken(): Token {
        var next = peek()

        // Skipping comments, newlines and whitespaces
        while (next == '\n' || next == ' ' || (next == '/' && peek(1) == '/')) {
            when (next) {
                ' ' -> advance()
                '\n' -> advance()
                else -> {
                    while ((next != null) && (next != '\n')) {
                        advance()
                        next = peek()
                    }
                }
            }
            next = peek()
        }

        if (tokenMapper.containsKey(next)) {
            return tokenMapper[next]!!.invoke()
        }

        // Parsing numbers
        if ((next!!.code < 58) && (next.code > 47)) {
            return parseNumber()
        }

        // Parsing keywords and identifiers
        if ((next == '_') || ((next.code > 64) && (next.code < 91)) ||
            (next.code > 96) && (next.code < 123)) {
            return parseLetters()
        }

        throw RuntimeException("Unexpected symbol encountered: $next")
    }

    private fun parseString(): Token {
        val value = StringBuilder()
        val startLine = line
        val startCol = col

        if (peek(1) == '"') { // Double-quoted string
            advance() // skip first "
            advance() // skip second "
            while ((peek() != '"') || (peek(1) != '"')) { // build String
                val symbol = advance()
                if (symbol == null) {
                    throw RuntimeException("String Literal not properly enclosed")
                }
                value.append(symbol)
            }
            advance() // skip closing "
            advance() // skip closing "
        } else { // Regular string parsing
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
            value.append(advance())
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

        while ((next == '_') || ((code > 64)
                    && (code < 91)) || (code > 96) && (code < 123)) {
            value.append(advance())
            next = peek()
            code = next?.code ?: 0
        }

        return if (value.toString() in KEYWORDS) {
            when (value.toString()) {
                "and" -> Token.AndOperator(c = startCol, l = startLine)
                "or" -> Token.OrOperator(c = startCol, l = startLine)
                "not" -> Token.NotOperator(c = startCol, l =startLine)
                else -> Token.Keyword(value = value.toString(), c = startCol, l = startLine)
            }
        } else if (value.toString() in BOOLEAN_VALUES) {
            Token.BooleanLiteral(value = value.toString() == "true", c = startCol, l = startLine)
        } else {
            Token.Identifier(value = value.toString(), c = startCol, l = startLine)
        }
    }
}