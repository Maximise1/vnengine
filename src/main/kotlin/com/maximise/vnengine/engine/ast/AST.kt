package com.maximise.vnengine.engine.ast

import kotlin.math.pow

sealed class VnNode(pos: SourcePos) {
    data class Program(
        val blocks: Map<String, Block>
    )

    data class Block(
        /**
         * Basically a unique path to the block that's formed by the Indexer after parsing
         */
        var id: String?,
        /**
         * A unique user-defined identifier made to simplify global state restoration during save export
         */
        val assignedId: String?,
        val name: String,
        val body: List<VnNode>,
        val blocks: Map<String, Block>,
        val pos: SourcePos
    ) : VnNode(pos)

    data class Dialogue(
        /**
         * Dialogue's index inside block
         */
        var blockIndex: Short?,
        val text: String,
        val speaker: String?,
        val pos: SourcePos
    ) : VnNode(pos)

    data class ExecuteStatement(
        val targetBlock: String,
        val pos: SourcePos
    ) : VnNode(pos)

    data class IgnoredStatement(
        val value: String,
        val pos: SourcePos
    ) : VnNode(pos)

    data class ChoiceStatement(
        val options: List<ChoiceOption>,
        val pos: SourcePos
    ) : VnNode(pos)

    data class IfStatement(
        val branches: List<IfBranch>,
        val elseBody: ElseBranch?,
        val pos: SourcePos
    ) : VnNode(pos)

    data class IfBranch(
        var id: String?,
        val assignedId: String?,
        val condition: Expression,
        val body: List<VnNode>
    )

    data class ElseBranch(
        var id: String?,
        val assignedId: String?,
        val body: List<VnNode>
    )

    data class ChoiceOption(
        var id: String?,
        val assignedId: String?,
        val expression: Expression?,
        val label: String,
        val body: List<VnNode>,
        val pos: SourcePos
    ) : VnNode(pos)

    data class AssignStatement(
        val variable: String,
        val value: Expression,
        val pos: SourcePos
    ) : VnNode(pos)
}

enum class UnaryExpression(val bp: Int) {
    NOT(9) {
        override fun apply(v: Value): Value {
            return when (v) {
                is Value.Num -> Value.Bool(v.v != 0.0)
                is Value.Str -> Value.Bool(v.v != "")
                is Value.Bool -> v
            }
        }
    };

    abstract fun apply(v: Value): Value
}

enum class BinaryExpression(val lbp: Int, val rbp: Int) { // TODO: add string concatenation as ..
    GREATER_EQUAL(3, 4) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Bool(l.v >= r.v)

                l is Value.Num && r is Value.Str -> {
                    Value.Bool(l.v >= r.asNumber())
                }

                l is Value.Str && r is Value.Num ->
                    Value.Bool(l.asNumber() >= r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Bool(l.asNumber() >= r.asNumber())

                else -> throw RuntimeException("Invalid operands for >=: ${l} and ${r}")
            }
    },
    EQUAL(1, 2) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Bool(l.v == r.v)

                l is Value.Num && r is Value.Str -> {
                    Value.Bool(l.v == r.asNumber())
                }

                l is Value.Str && r is Value.Num ->
                    Value.Bool(l.asNumber() == r.v)

                l is Value.Bool && r is Value.Bool ->
                    Value.Bool(l.v == r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Bool(l.v == r.v)

                l is Value.Bool && r is Value.Str ->
                    Value.Bool(l.v == r.asBool())

                l is Value.Str && r is Value.Bool ->
                    Value.Bool(l.asBool() == r.v)

                else -> throw RuntimeException("Invalid operands for ==: ${l} and ${r}")
            }
    },
    LESS_EQUAL(3, 4) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Bool(l.v <= r.v)

                l is Value.Num && r is Value.Str -> {
                    Value.Bool(l.v <= r.asNumber())
                }

                l is Value.Str && r is Value.Num ->
                    Value.Bool(l.asNumber() <= r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Bool(l.asNumber() <= r.asNumber())

                else -> throw RuntimeException("Invalid operands for <=: ${l} and ${r}")
            }
    },
    GREATER(3, 4) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Bool(l.v > r.v)

                l is Value.Num && r is Value.Str -> {
                    Value.Bool(l.v > r.asNumber())
                }

                l is Value.Str && r is Value.Num ->
                    Value.Bool(l.asNumber() > r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Bool(l.asNumber() > r.asNumber())

                else -> throw RuntimeException("Invalid operands for >: ${l} and ${r}")
            }
    },
    LESS(3, 4) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Bool(l.v < r.v)

                l is Value.Num && r is Value.Str -> {
                    Value.Bool(l.v < r.asNumber())
                }

                l is Value.Str && r is Value.Num ->
                    Value.Bool(l.asNumber() < r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Bool(l.asNumber() < r.asNumber())

                else -> throw RuntimeException("Invalid operands for <: ${l} and ${r}")
            }
    },
    AND(5, 6) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Bool && r is Value.Bool ->
                    Value.Bool(l.v && r.v)

                l is Value.Bool && r is Value.Str -> {
                    Value.Bool(l.v && r.asBool())
                }

                l is Value.Str && r is Value.Bool ->
                    Value.Bool(l.asBool() && r.asBool())

                else -> throw RuntimeException("Invalid operands for \"and\": ${l} and ${r}")
            }
    },
    OR(5, 6) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Bool && r is Value.Bool ->
                    Value.Bool(l.v || r.v)

                l is Value.Bool && r is Value.Str -> {
                    Value.Bool(l.v || r.asBool())
                }

                l is Value.Str && r is Value.Bool ->
                    Value.Bool(l.asBool() || r.asBool())

                else -> throw RuntimeException("Invalid operands for \"or\": ${l} and ${r}")
            }
    },
    PLUS(5, 6) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Num(l.v + r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Num(l.asNumber() + r.asNumber())

                l is Value.Str && r is Value.Num ->
                    Value.Num(l.asNumber() + r.v)

                l is Value.Num && r is Value.Str ->
                    Value.Num(l.v + r.asNumber())

                else -> throw RuntimeException("Invalid operands for +: ${l} and ${r}")
            }
    },
    MINUS(5, 6) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Num(l.v - r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Num(l.asNumber() - r.asNumber())

                l is Value.Str && r is Value.Num ->
                    Value.Num(l.asNumber() - r.v)

                l is Value.Num && r is Value.Str ->
                    Value.Num(l.v - r.asNumber())

                else -> throw RuntimeException("Invalid operands for -: ${l} and ${r}")
            }
    },
    DIV(7, 8) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Num(l.v / r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Num(l.asNumber() / r.asNumber())

                l is Value.Str && r is Value.Num ->
                    Value.Num(l.asNumber() / r.v)

                l is Value.Num && r is Value.Str ->
                    Value.Num(l.v / r.asNumber())

                else -> throw RuntimeException("Invalid operands for /: ${l} and ${r}")
            }
    },
    REM(7, 8) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Num(l.v % r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Num(l.asNumber() % r.asNumber())

                l is Value.Str && r is Value.Num ->
                    Value.Num(l.asNumber() % r.v)

                l is Value.Num && r is Value.Str ->
                    Value.Num(l.v % r.asNumber())

                else -> throw RuntimeException("Invalid operands for %: ${l} and ${r}")
            }
    },
    MUL(7, 8) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Num(l.v * r.v)

                l is Value.Str && r is Value.Str ->
                    Value.Num(l.asNumber() * r.asNumber())

                l is Value.Str && r is Value.Num ->
                    Value.Num(l.asNumber() * r.v)

                l is Value.Num && r is Value.Str ->
                    Value.Num(l.v * r.asNumber())

                else -> throw RuntimeException("Invalid operands for *: ${l} and ${r}")
            }
    },
    POW(10, 11) {
        override fun apply(l: Value, r: Value): Value =
            when {
                l is Value.Num && r is Value.Num ->
                    Value.Num(l.v.pow(r.v))

                l is Value.Str && r is Value.Str ->
                    Value.Num(l.asNumber().pow(r.asNumber()))

                l is Value.Str && r is Value.Num ->
                    Value.Num(l.asNumber().pow(r.v))

                l is Value.Num && r is Value.Str ->
                    Value.Num(l.v.pow(r.asNumber()))

                else -> throw RuntimeException("Invalid operands for **: ${l} and ${r}")
            }
    };

    abstract fun apply(l: Value, r: Value): Value
}

sealed class Expression(
    val pos: SourcePos
) {
    data class BooleanLiteral(
        val value: Boolean,
        val p: SourcePos
    ) : Expression(p)

    data class StringLiteral(
        val value: String,
        val p: SourcePos
    ) : Expression(p)

    data class NumberLiteral(
        val value: Double,
        val p: SourcePos
    ) : Expression(p)

    data class Variable(
        val name: String,
        val p: SourcePos
    ) : Expression(p)

    data class BinaryOperator(
        val left: Expression,
        val right: Expression,
        val operator: BinaryExpression,
        val p: SourcePos
    ) : Expression(p)

    data class UnaryOperator(
        val expression: Expression,
        val operator: UnaryExpression,
        val p: SourcePos
    ) : Expression(p)
}

data class SourcePos(
    val line: Int,
    val column: Int
)