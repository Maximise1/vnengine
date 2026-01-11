package com.maximise.vnengine.engine.interpreter

import com.maximise.vnengine.engine.parser.SourcePos
import com.maximise.vnengine.engine.parser.VnNode

class ExecutionContext(
    val blocks: Map<String, VnNode.Block>
) {
    val stack: ArrayDeque<ExecutionFrame> = ArrayDeque()

    fun currentBlock(): VnNode.Block? {
        if (!stack.isEmpty()) {
            return stack.last().block
        } else {
            return null
        }
    }

    fun currentIndex(): Int? {
        if (!stack.isEmpty()) {
            return stack.last().currentIndex
        } else {
            return null
        }
    }

    fun pushBlock(block: VnNode.Block, index: Int = 0) {
        stack.add(
            ExecutionFrame(
                block = block,
                currentIndex = index,
                stringVariables = mutableMapOf(),
                booleanVariables = mutableMapOf(),
                numberVariables = mutableMapOf()
            )
        )
    }

    fun popBlock() {
        stack.removeLast()

        if (stack.isEmpty()) {
            return
        }

        while (stack.last().block.expressions.size <= stack.last().currentIndex + 1) {
            stack.removeLast()

            if (stack.isEmpty()) {
                return
            }
        }

        stack.last().currentIndex += 1
    }

    fun findBlock(name: String): VnNode.Block? {
        val lastBlock = stack.lastOrNull()

        lastBlock?.let {
            if (it.block.blocks.containsKey(name)) {
                return it.block.blocks[name]
            }
        }

        if (blocks.containsKey(name)) {
            return blocks[name]
        }

        return null
    }

    fun incrementIndex() {
        if (currentBlock()!!.expressions.size - 1 > currentIndex()!!) {
            stack.last().currentIndex++
        } else {
            popBlock()
        }
    }

    /*fun moveIndex(i: Int) {
        val currentBlock = stack.last()

        if (currentBlock.currentIndex + i < 0) {
            var currentBlockIndex = stack.size - 1
            var currentI = i + currentBlock.currentIndex + 1

            while (stack[currentBlockIndex].currentIndex + currentI < 0) {
                stack.removeLast()
                currentI = currentI + stack[currentBlockIndex].currentIndex + 1
                currentBlockIndex--

                if (currentBlockIndex < 0) {
                    currentI = 0
                    break
                }
            }

            stack.last().currentIndex = 0
        }
    }*/
}