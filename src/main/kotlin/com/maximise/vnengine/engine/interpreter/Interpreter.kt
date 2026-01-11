package com.maximise.vnengine.engine.interpreter

import com.maximise.vnengine.engine.parser.VnNode

class Interpreter {

    private var context = ExecutionContext(
        blocks = mutableMapOf(),
    )

    fun run(program: VnNode.Program) {
        context = ExecutionContext(
            blocks = program.blocks
        )

        val startBlock = context.findBlock("start")
        if (startBlock == null) {
            throw RuntimeException("Program must contain \"start\" block to begin execution")
        }
        context.pushBlock(startBlock)

        runProgram()
    }

    private fun runProgram() {
        while (!context.stack.isEmpty()) {
            advance()
        }
    }

    private fun advance() {
        val node = context.currentBlock()!!.expressions[context.currentIndex()!!]

        when (node) {
            is VnNode.Dialogue -> executeDialogue(node)
            is VnNode.ExecuteStatement -> executeBlock(node.targetBlock)
            is VnNode.IgnoredStatement -> executeIgnoredStatement(node)
            else -> throw RuntimeException("Unexpected VnNode encountered: $node")
        }
    }

    private fun executeBlock(blockName: String) {
        val block = context.findBlock(blockName)

        if (block == null) throw RuntimeException("Block with name $blockName not found")

        context.pushBlock(block)
    }

    private fun executeDialogue(dialogue: VnNode.Dialogue) {
        println("${dialogue.speaker}: ${dialogue.text}")
        context.incrementIndex()
    }

    private fun executeIgnoredStatement(node: VnNode.IgnoredStatement) {
        println("Warning. This value was ignored during compilation: ${node.value}")
        context.incrementIndex()
    }

    private fun rollback() {
        // TODO: Move back in nodes until dialogue or smth else is met
    }
}