package com.maximise.vnengine.engine.parser

sealed class VnNode(pos: SourcePos) {
    data class Program(
        val blocks: Map<String, Block>
    )

    data class Block(
        val name: String,
        val expressions: List<VnNode>,
        val blocks: Map<String, Block>,
        val pos: SourcePos
    ) : VnNode(pos)

    data class Dialogue(
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
}

data class SourcePos(
    val line: Int,
    val column: Int
)