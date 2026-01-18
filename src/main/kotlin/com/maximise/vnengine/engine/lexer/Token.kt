package com.maximise.vnengine.engine.lexer

sealed class Token(val line: Int, val col: Int) {
    data class StringLiteral(val value: String, val l: Int, val c: Int) : Token(l, c)
    data class NumberLiteral(val value: Double, val l: Int, val c: Int) : Token(l, c)
    data class BooleanLiteral(val value: Boolean, val l: Int, val c: Int) : Token(l, c)
    data class Keyword(val value: String, val c: Int, val l: Int) : Token(l, c)
    data class Identifier(val value: String, val c: Int, val l: Int): Token(l, c)
    object EOF : Token(-1, -1)

    // parenthesis and braces
    data class OpenParenthesis(val l: Int, val c: Int) : Token(l, c)
    data class CloseParenthesis(val l: Int, val c: Int) : Token(l, c)
    data class OpenBraces(val l: Int, val c: Int) : Token(l, c)
    data class CloseBraces(val l: Int, val c: Int) : Token(l, c)

    // Operators
    data class AssignOperator(val l: Int, val c: Int) : Token(l, c)
    data class EqualsOperator(val l: Int, val c: Int) : Token(l, c)
    data class GreaterOperator(val l: Int, val c: Int) : Token(l, c)
    data class GreaterOrEqualOperator(val l: Int, val c: Int) : Token(l, c)
    data class LessOperator(val l: Int, val c: Int) : Token(l, c)
    data class LessOrEqualOperator(val l: Int, val c: Int) : Token(l, c)
    data class AndOperator(val l: Int, val c: Int) : Token(l, c)
    data class OrOperator(val l: Int, val c: Int) : Token(l, c)
    data class PlusOperator(val l: Int, val c: Int) : Token(l, c)
    data class MinusOperator(val l: Int, val c: Int) : Token(l, c)
    data class DivOperator(val l: Int, val c: Int) : Token(l, c)
    data class RemOperator(val l: Int, val c: Int) : Token(l, c)
    data class MulOperator(val l: Int, val c: Int) : Token(l, c)
    data class PowOperator(val l: Int, val c: Int) : Token(l, c)
    data class NotOperator(val l: Int, val c: Int) : Token(l, c)
}

val KEYWORDS = listOf("block", "execute", "choice", "if", "else", "and", "or", "not")
val BOOLEAN_VALUES = listOf("true", "false")