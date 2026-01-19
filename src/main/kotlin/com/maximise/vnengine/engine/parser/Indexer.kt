package com.maximise.vnengine.engine.parser

import com.maximise.vnengine.engine.ast.VnNode

class Indexer {
    val blockNameStack: ArrayDeque<String> = ArrayDeque()

    fun indexProgram(program: VnNode.Program): VnNode.Program {
        program.blocks.forEach { (blockName, block) ->
            blockNameStack.add(blockName)
            indexBlock(block)
            blockNameStack.removeLast()
        }
        return program
    }

    private fun indexBlock(block: VnNode.Block) {
        val blockPath = currentPath()

        indexBlockBody(block.body, blockPath)
        block.id = blockPath

        block.blocks.forEach { (blockName, nestedBlock) ->
            blockNameStack.add(blockName)
            indexBlock(nestedBlock)
            blockNameStack.removeLast()
        }
    }

    private fun currentPath(): String = blockNameStack.joinToString("/")

    private fun indexBlockBody(body: List<VnNode>, parentPath: String) {
        var localIfCounter = 0
        var localElseCounter = 0
        var localChoiceCounter = 0
        var dialogueCounter: Short = 0

        body.forEach { node ->
            when (node) {
                is VnNode.Dialogue -> {
                    node.blockIndex = dialogueCounter
                    dialogueCounter++
                }

                is VnNode.IfStatement -> {
                    node.branches.forEachIndexed { branchIdx, branch ->
                        localIfCounter++
                        val branchPath = "$parentPath/if$localIfCounter-branch$branchIdx"
                        branch.id = branchPath

                        indexBlockBody(branch.body, branchPath)
                    }

                    node.elseBody?.let { elseBranch ->
                        localElseCounter++
                        val elsePath = "$parentPath/if$localIfCounter-else"
                        elseBranch.id = elsePath

                        indexBlockBody(elseBranch.body, elsePath)
                    }
                }

                is VnNode.ChoiceStatement -> {
                    node.options.forEachIndexed { optionIdx, option ->
                        localChoiceCounter++
                        val choicePath = "$parentPath/choice$localChoiceCounter-option$optionIdx"
                        option.id = choicePath

                        indexBlockBody(option.body, choicePath)
                    }
                }

                else -> {}
            }
        }
    }
}