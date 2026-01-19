package com.maximise.vnengine.engine.runtime

import com.maximise.vnengine.engine.ast.VnNode

data class ExecutionFrame(
    val blockHash: String,
    val block: VnNode.Block,
    var currentIndex: Int
)