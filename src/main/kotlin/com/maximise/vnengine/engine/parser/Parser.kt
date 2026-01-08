package com.maximise.vnengine.engine.parser

import com.maximise.vnengine.engine.lexer.Token

class Parser {
    private var cursor: Int = 0
    private var tokens: List<Token> = listOf()

    private fun peek(offset: Int = 0): Token? {
        return if (tokens.size <= cursor + offset) {
            null
        } else {
            tokens[cursor + offset]
        }
    }

    private fun advance(): Token? {
        return if (tokens.size <= cursor) {
            null
        } else {
            val token = tokens[cursor]
            cursor++
            //println("Parsing token $token")
            token
        }
    }

    fun parseProgram(inputTokens: List<Token>): VnNode.Program {
        cursor = 0
        tokens = inputTokens
        val blocks = mutableMapOf<String, VnNode.Block>()

        while (peek() != null && peek() != Token.EOF) {
            val block = parseBlock()
            if (blocks.containsKey(block.name)) {
                throw RuntimeException("Duplicate block: ${block.name}")
            }
            blocks.put(key = block.name, value = block)
        }

        return VnNode.Program(
            blocks = blocks
        )
    }

    private fun parseBlock(): VnNode.Block {
        var token = advance()
        val blockContents = mutableListOf<VnNode>()
        val innerBlocks = mutableMapOf<String, VnNode.Block>()
        if (!(token is Token.Keyword && token.value == "block")) {
            throw RuntimeException("Every block must start with a \"block\" keyword")
        }

        token = advance()
        if (token !is Token.Identifier) {
            throw RuntimeException("\"block\" keyword must be followed with a block name. Example: block empty_block {}")
        }
        val blockName = token.value
        val blockPos = SourcePos(
            line = token.line,
            column = token.col
        )

        token = advance()
        if (token !is Token.OpenBracket) {
            throw RuntimeException("Every block must start with an open bracket. Example: block empty_block {}")
        }

        while (peek() !is Token.CloseBracket) {
            token = peek()
            if (token == null || token is Token.EOF) {
                throw RuntimeException("Every block must end with a close bracket. Example: block empty_block {}")
            }

            // Random numbers that don't fit anywhere become ignored statements for later warning
            if (token is Token.NumberLiteral) {
                blockContents.add(VnNode.IgnoredStatement(
                    value = token.value.toString(),
                    pos = SourcePos(
                        line = token.line,
                        column = token.col
                    )
                ))
                advance()
                continue
            }

            //println(token)

            val node = when (token) {
                is Token.Keyword -> parseKeyword()
                is Token.StringLiteral -> parseDialogue()
                is Token.Identifier -> parseDialogue()
                is Token.OpenBracket -> throw RuntimeException("Unexpected open bracket encountered")
                Token.EOF -> throw RuntimeException("Unexpected end of file encountered")
                else -> throw RuntimeException("Unexpected token encountered: $token")
            }

            if (node is VnNode.Block) {
                if (innerBlocks.containsKey(node.name)) {
                    throw RuntimeException("Duplicate block: ${node.name}")
                }
                innerBlocks.put(
                    key = node.name,
                    value = node
                )
            } else {
                blockContents.add(node)
            }
        }

        advance()

        return VnNode.Block(
            name = blockName,
            expressions = blockContents,
            blocks = innerBlocks,
            pos = blockPos
        )
    }

    private fun parseKeyword(): VnNode {
        val keyword = advance() as Token.Keyword

        return when (keyword.value) {
            "block" -> parseBlock()
            "execute" -> parseExecute()
            else -> throw RuntimeException("Unexpected keyword: ${keyword.value}")
        }
    }

    private fun parseExecute(): VnNode.ExecuteStatement {
        advance()
        val blockName = advance()

        if (blockName !is Token.Identifier) {
            throw RuntimeException("Execute statement followed by wrong token: $blockName")
        } else {
            return VnNode.ExecuteStatement(
                targetBlock = blockName.value,
                pos = SourcePos(
                    line = blockName.line,
                    column = blockName.col
                )
            )
        }
    }

    private fun parseDialogue(): VnNode.Dialogue {
        val node = advance()

        if (node is Token.StringLiteral) {
            return VnNode.Dialogue(
                text = node.value,
                speaker = null,
                pos = SourcePos(
                    line = node.line,
                    column = node.col
                )
            )
        } else if (node is Token.Identifier) {
            val text = advance()
            if (text !is Token.StringLiteral) {
                throw RuntimeException("Failed to parse dialogue with identifier ${node.value}")
            } else {
                return VnNode.Dialogue(
                    text = text.value,
                    speaker = node.value,
                    pos = SourcePos(
                        line = node.line,
                        column = node.col
                    )
                )
            }
        } else {
            throw RuntimeException("Failed to parse dialogue with token $node")
        }
    }
}