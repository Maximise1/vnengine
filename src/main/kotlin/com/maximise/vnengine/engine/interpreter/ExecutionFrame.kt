package com.maximise.vnengine.engine.interpreter

import com.maximise.vnengine.engine.parser.VnNode

data class ExecutionFrame(
    val block: VnNode.Block,
    var currentIndex: Int,
    val stringVariables: MutableMap<String, String>,
    val booleanVariables: MutableMap<String, Boolean>,
    val numberVariables: MutableMap<String, Double>
)
