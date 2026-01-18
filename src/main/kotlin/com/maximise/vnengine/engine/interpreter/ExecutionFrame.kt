package com.maximise.vnengine.engine.interpreter

import com.maximise.vnengine.engine.parser.VnNode

data class ExecutionFrame(
    val blockId: Int,
    val block: VnNode.Block,
    var currentIndex: Int
)
