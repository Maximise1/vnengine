package com.maximise.vnengine.engine.runtime

import com.maximise.vnengine.engine.ast.Value
import com.maximise.vnengine.engine.ast.VnNode

class ExecutionContext(
    val blocks: Map<String, VnNode.Block>
) {

    val stack: ArrayDeque<ExecutionFrame> = ArrayDeque()
    val variables: MutableMap<String, Variable> = mutableMapOf()
    val seenDialogue: MutableMap<String, Short> = indexBlocksWithDialogue()

    var currentBlockId: String = "" // TODO: remove this abomination. It's only used during seen dialogue checks.

    fun isDialogueSeen(index: Short): Boolean
        = seenDialogue[currentBlockId]!! > index

    private fun indexBlocksWithDialogue(): MutableMap<String, Short> {
        val indexes = mutableMapOf<String, Short>()
        blocks.forEach { name, block ->
            indexes.putAll(mapBlock(block))
        }
        return indexes
    }

    private fun mapBlock(block: VnNode.Block): MutableMap<String, Short> {
        val indexes = mutableMapOf<String, Short>()
        block.blocks.forEach { name, block ->
            indexes.putAll(mapBlock(block))
        }

        block.body.forEach { node ->
            when (node) {
                is VnNode.IfStatement -> {
                    node.branches.forEach { branch ->
                        indexes.put(branch.assignedId ?: branch.id!!, 0)
                    }
                    if (node.elseBody != null) {
                        indexes.put(node.elseBody.assignedId ?: node.elseBody.id!!, 0)
                    }
                }
                is VnNode.ChoiceStatement -> {
                    node.options.forEach { option ->
                        indexes.put(option.assignedId ?: option.id!!, 0)
                    }
                }
                else -> {}
            }
        }

        indexes.put(block.id ?: block.assignedId!!, 0)

        return indexes
    }

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
                blockHash = block.id!!
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

    fun incrementIndex(
        dialogueIndex: Short = 0
    ) {
        val block = currentBlock()!!
        val blockId = block.assignedId ?: block.id!!
        currentBlockId = blockId
        if (seenDialogue[blockId]!! < dialogueIndex) {
            seenDialogue[blockId] = dialogueIndex
        }

        if (block.body.size - 1 > currentIndex()!!) {
            stack.last().currentIndex++
        } else {
            popBlock()
        }
    }
}