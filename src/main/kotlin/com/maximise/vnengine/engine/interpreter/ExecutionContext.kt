package com.maximise.vnengine.engine.interpreter

import com.maximise.vnengine.engine.parser.VnNode

class ExecutionContext(
    val blocks: Map<String, VnNode.Block>
) {

    val stack: ArrayDeque<ExecutionFrame> = ArrayDeque()
    val variables: MutableMap<String, Variable> = mutableMapOf()

    /* fun toSaveSafeExecutionFrameStack(): ArrayDeque<Pair<Int, Int>> {
        return // remove block from each ExecutionFrame
    } */

    fun currentBlock(): VnNode.Block? {
        return if (!stack.isEmpty()) {
            stack.last().block
        } else {
            null
        }
    }

    fun setVariable(name: String, value: String) {
        variables[name] = Variable(
            name = name,
            value = Value.Str(value)
        )
    }

    fun setVariable(name: String, value: Double) {
        variables[name] = Variable(
            name = name,
            value = Value.Num(value)
        )
    }

    fun setVariable(name: String, value: Boolean) {
        variables[name] = Variable(
            name = name,
            value = Value.Bool(value)
        )
    }

    fun getVariable(name: String): Variable? {
        return variables[name]
    }

    fun currentIndex(): Int? {
        return if (!stack.isEmpty()) {
            stack.last().currentIndex
        } else {
            null
        }
    }

    fun pushBlock(block: VnNode.Block, index: Int = 0) {
        stack.add(
            ExecutionFrame(
                block = block,
                currentIndex = index,
                blockId = block.id
            )
        )
    }

    fun popBlock() {
        stack.removeLast()

        if (stack.isEmpty()) {
            return
        }

        while (stack.last().block.body.size <= stack.last().currentIndex + 1) {
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
        if (currentBlock()!!.body.size - 1 > currentIndex()!!) {
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