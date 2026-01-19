package com.maximise.vnengine.engine.ast

sealed interface Value {
    data class Str(val v: String) : Value
    data class Num(val v: Double) : Value
    data class Bool(val v: Boolean) : Value
}

fun Value.asNumber(): Double =
    when (this) {
        is Value.Num -> v
        is Value.Str -> {
            val value = v.toDoubleOrNull()
            if (value == null) {
                throw RuntimeException("String $v can't be converted to Number type")
            }
            value
        }
        is Value.Bool -> {
            throw RuntimeException("Boolean $v can't be converted to Number type")
        }
    }

fun Value.asBool(): Boolean =
    when (this) {
        is Value.Bool -> v
        is Value.Str -> {
            if (v.lowercase() == "false") {
                false
            } else if (v.lowercase() == "true") {
                true
            } else {
                throw RuntimeException("String $v can't be converted to Boolean type")
            }
        }
        is Value.Num -> {
            throw RuntimeException("Number $v can't be converted to Boolean type")
        }
    }