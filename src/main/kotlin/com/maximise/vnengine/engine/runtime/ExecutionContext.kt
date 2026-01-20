package com.maximise.vnengine.engine.runtime

import com.maximise.vnengine.engine.ast.Value
import com.maximise.vnengine.engine.ast.VnNode

class ExecutionContext(
    val blocks: Map<String, VnNode.Block>,
    dialogue: MutableMap<String, Short>,
    savedStack: List<Pair<String, Int>>,
    savedVariables: MutableMap<String, Value>,
    persistentVals: MutableMap<String, Value>
) {

    val stack: ArrayDeque<ExecutionFrame> = reconstructStack(savedStack)
    val variables: MutableMap<String, Value> = savedVariables
    val seenDialogue: MutableMap<String, Short> = dialogue.ifEmpty { indexBlocksWithDialogue() }
    val persistentValues: MutableMap<String, Value> = persistentVals

    var currentBlockId: String = "" // TODO: remove this abomination. It's only used during seen dialogue checks.

    fun isDialogueSeen(index: Short): Boolean
        = seenDialogue[currentBlockId]!! > index

    private fun reconstructStack(savedStack: List<Pair<String, Int>>): ArrayDeque<ExecutionFrame> {
        if (savedStack.isEmpty()) return ArrayDeque()

        val stack: ArrayDeque<ExecutionFrame> = ArrayDeque(savedStack.size)
        val hashSet: Set<String> = (savedStack.map { pair -> pair.first }).toSet()
        val foundBlocks: MutableList<VnNode.Block> = mutableListOf()

        blocks.forEach { block ->
            foundBlocks.addAll(getBlocksByHash(hashSet, block.value))
        }

        savedStack.forEach { (hash, index) ->
            stack.add(ExecutionFrame(
                blockHash = hash,
                block = foundBlocks.find { block -> (block.assignedId ?: block.id) == hash }!!,
                currentIndex = index
            ))
        }

        return stack
    }

    private fun getBlocksByHash(hashes: Set<String>, block: VnNode.Block): List<VnNode.Block> {
        val blocks: MutableList<VnNode.Block> = mutableListOf()

        block.blocks.forEach { nameAndBlock ->
            blocks.addAll(getBlocksByHash(hashes, nameAndBlock.value))
        }

        if (hashes.contains(block.id) || hashes.contains(block.assignedId)) {
            blocks.add(block)
        }

        block.body.forEach { node ->
            when (node) {
                is VnNode.IfStatement -> {
                    node.branches.forEach { branch ->
                        blocks.addAll(getBlocksByHash(
                            hashes,
                            VnNode.Block(
                                id = branch.id,
                                assignedId = branch.assignedId,
                                name = "",
                                body = branch.body,
                                blocks = mutableMapOf(),
                                pos = node.pos
                            )
                        ))
                        if (hashes.contains(branch.assignedId) || hashes.contains(branch.id)) {
                            blocks.add(VnNode.Block(
                                id = branch.id,
                                assignedId = branch.assignedId,
                                name = "",
                                body = branch.body,
                                blocks = mutableMapOf(),
                                pos = node.pos
                            ))
                        }
                    }
                    if (node.elseBody != null) {
                        blocks.addAll(getBlocksByHash(
                            hashes,
                            VnNode.Block(
                                id = node.elseBody.id,
                                assignedId = node.elseBody.assignedId,
                                name = "",
                                body = node.elseBody.body,
                                blocks = mutableMapOf(),
                                pos = node.pos
                            )
                        ))
                        if (hashes.contains(node.elseBody.assignedId) || hashes.contains(node.elseBody.id)) {
                            blocks.add(VnNode.Block(
                                id = node.elseBody.id,
                                assignedId = node.elseBody.assignedId,
                                name = "",
                                body = node.elseBody.body,
                                blocks = mutableMapOf(),
                                pos = node.pos
                            ))
                        }
                    }
                }
                is VnNode.ChoiceStatement -> {
                    node.options.forEach { option ->
                        blocks.addAll(getBlocksByHash(
                            hashes,
                            VnNode.Block(
                                id = option.id,
                                assignedId = option.assignedId,
                                name = "",
                                body = option.body,
                                blocks = mutableMapOf(),
                                pos = option.pos
                            )
                        ))
                        if (hashes.contains(option.assignedId) || hashes.contains(option.id)) {
                            blocks.add(VnNode.Block(
                                id = option.id,
                                assignedId = option.assignedId,
                                name = "",
                                body = option.body,
                                blocks = mutableMapOf(),
                                pos = option.pos
                            ))
                        }
                    }
                }
                else -> {}
            }
        }

        return blocks
    }

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
                        indexes.putAll(mapBlock(VnNode.Block(
                            id = branch.id,
                            assignedId = branch.assignedId,
                            name = "",
                            body = branch.body,
                            blocks = mutableMapOf(),
                            pos = node.pos
                        )))
                    }
                    if (node.elseBody != null) {
                        indexes.put(node.elseBody.assignedId ?: node.elseBody.id!!, 0)
                        indexes.putAll(mapBlock(VnNode.Block(
                            id = node.elseBody.id,
                            assignedId = node.elseBody.assignedId,
                            name = "",
                            body = node.elseBody.body,
                            blocks = mutableMapOf(),
                            pos = node.pos
                        )))
                    }
                }
                is VnNode.ChoiceStatement -> {
                    node.options.forEach { option ->
                        indexes.put(option.assignedId ?: option.id!!, 0)
                        indexes.putAll(mapBlock(VnNode.Block(
                            id = option.id,
                            assignedId = option.assignedId,
                            name = "",
                            body = option.body,
                            blocks = mutableMapOf(),
                            pos = node.pos
                        )))
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
        variables[name] = Value.Str(value)
    }

    fun setVariable(name: String, value: Double) {
        variables[name] = Value.Num(value)
    }

    fun setVariable(name: String, value: Boolean) {
        variables[name] = Value.Bool(value)
    }

    fun getVariable(name: String): Value? {
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