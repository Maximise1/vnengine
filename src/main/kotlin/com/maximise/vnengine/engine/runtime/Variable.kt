package com.maximise.vnengine.engine.runtime

import com.maximise.vnengine.engine.ast.Value

data class Variable(
    val name: String,
    val value: Value
)