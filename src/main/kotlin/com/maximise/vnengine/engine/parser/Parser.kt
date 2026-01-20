package com.maximise.vnengine.engine.parser

import com.maximise.vnengine.engine.ast.BinaryExpression
import com.maximise.vnengine.engine.ast.Expression
import com.maximise.vnengine.engine.ast.SourcePos
import com.maximise.vnengine.engine.ast.UnaryExpression
import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.lexer.Token
import kotlin.reflect.KClass
import kotlin.to

class Parser(
    private val indexer: Indexer
) {

    private val operators: Map<KClass<out Token>, BinaryExpression> = mapOf(
        Token.EqualsOperator::class to BinaryExpression.EQUAL,
        Token.GreaterOperator::class to BinaryExpression.GREATER,
        Token.GreaterOrEqualOperator::class to BinaryExpression.GREATER_EQUAL,
        Token.LessOperator::class to BinaryExpression.LESS,
        Token.LessOrEqualOperator::class to BinaryExpression.LESS_EQUAL,
        Token.AndOperator::class to BinaryExpression.AND,
        Token.OrOperator::class to BinaryExpression.OR,
        Token.PlusOperator::class to BinaryExpression.PLUS,
        Token.DivOperator::class to BinaryExpression.DIV,
        Token.MinusOperator::class to BinaryExpression.MINUS,
        Token.RemOperator::class to BinaryExpression.REM,
        Token.MulOperator::class to BinaryExpression.MUL,
        Token.PowOperator::class to BinaryExpression.POW
    )

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

        return indexer.indexProgram(
            VnNode.Program(
            blocks = blocks
        ))
    }

    private fun parseIdentifier(): VnNode {
        val afterNext = peek(1)
        if (afterNext is Token.AssignOperator) {
            return parseAssignStatement()
        } else if (afterNext is Token.StringLiteral) {
            return parseDialogue()
        } else {
            throw RuntimeException(
                "Identifier ${peek()} followed by unexpected syntax: ${peek(1)}")
        }
    }

    private fun parseAssignStatement(): VnNode.AssignStatement {
        val variable = advance()
        if (variable is Token.Identifier) {
            advance()
            val expression = parseExpression()
            return VnNode.AssignStatement(
                variable = variable.value,
                value = expression,
                pos = SourcePos(
                    line = variable.line,
                    column = variable.col
                )
            )
        }
        throw RuntimeException(
            "Error during assign statement parsing. Unexpected token: $variable")
    }

    private fun parseOperand(): Expression {
        val token = advance()

        val operand = when (token) {
            is Token.OpenParenthesis ->  {
                val expr = parseExpression()
                val closeBracket = advance()
                if (closeBracket !is Token.CloseParenthesis) {
                    throw RuntimeException("Unclosed parenthesis: $token")
                }
                expr
            }

            is Token.NotOperator -> {
                val expr = parseExpression(UnaryExpression.NOT.bp)
                Expression.UnaryOperator(
                    expression = expr,
                    operator = UnaryExpression.NOT,
                    p = SourcePos(token.line, token.col)
                )
            }

            is Token.Identifier -> Expression.Variable(
                name = token.value,
                p = SourcePos(
                    line = token.line,
                    column = token.col
                )
            )

            is Token.StringLiteral -> Expression.StringLiteral(
                value = token.value,
                p = SourcePos(
                    line = token.line,
                    column = token.col
                )
            )

            is Token.NumberLiteral -> Expression.NumberLiteral(
                value = token.value,
                p = SourcePos(
                    line = token.line,
                    column = token.col
                )
            )

            is Token.BooleanLiteral -> Expression.BooleanLiteral(
                value = token.value,
                p = SourcePos(
                    line = token.line,
                    column = token.col
                )
            )

            else -> throw RuntimeException("Unexpected type of literal: $token")
        }

        return operand
    }

    private fun peekOperator(): BinaryExpression? {
        val token = peek() ?: return null
        return operators[token::class]
    }

    // 1 + 2 * 3 - 4
    // lE = 1; operator = +; false; consumed operator; sO = 2 * 3; lE = 1 + (2 * 3); operator = -; true; sO = 4; lE = (1 + (2 * 3)) - 4; returned lE
    // lE = 2; operator = *; false; consumed operator; sO = 3; lE = 2 * 3; operator = -; false; returned 2 * 3;
    // lE = 3; operator = -; true; returned 3;
    private fun parseExpression(prevBindingPower: Int = 0): Expression {
        var leftExpression = parseOperand()

        while (true) {
            val operator = peekOperator()
            if (operator == null) break

            if (operator.lbp < prevBindingPower) break

            advance()
            val secondOperand = parseExpression(operator.rbp)

            leftExpression = Expression.BinaryOperator(
                left = leftExpression,
                right = secondOperand,
                operator = operator,
                p = leftExpression.pos
            )
        }

        return leftExpression
    }

    private fun parseKeyword(): VnNode {
        val keyword = advance() as Token.Keyword

        return when (keyword.value) {
            "block" -> parseBlock()
            "execute" -> parseExecute()
            "if" -> parseIf()
            "else" -> throw RuntimeException("else keyword can't be on it's own")
            "choice" -> parseChoice()
            else -> throw RuntimeException("Unexpected keyword: ${keyword.value}")
        }
    }

    private fun parseIf(): VnNode.IfStatement {
        val branches = mutableListOf<VnNode.IfBranch>()
        var elseBody: List<VnNode>? = null

        branches.add(parseIfBranch(

        ))


        var next = peek()
        while (next is Token.Keyword && next.value == "else") {
            advance() // Consumed else

            val afterNext = peek()
            if (afterNext is Token.Keyword && afterNext.value == "if") {
                advance() // Consumed if
                branches.add(parseIfBranch())
            } else {
                elseBody = parseBlockBody().first
                break
            }
            next = peek()
        }

        return VnNode.IfStatement(
            branches = branches,
            elseBody = elseBody?.let { VnNode.ElseBranch(
                id = null,
                assignedId = null,
                body = elseBody
            ) },
            pos = branches.first().condition.pos
        )
    }

    private fun parseIfBranch(): VnNode.IfBranch {
        val condition = parseExpression()
        val body = parseBlockBody()

        return VnNode.IfBranch(
            id = null,
            assignedId = null,
            condition = condition,
            body = body.first
        )
    }

    private fun parseBlock(): VnNode.Block {
        var token = advance()
        if (!(token is Token.Keyword && token.value == "block")) {
            throw RuntimeException("Every block must start with a \"block\" keyword")
        }

        token = advance()
        if (token !is Token.Identifier) {
            throw RuntimeException("\"block\" keyword must be followed with " +
                    "a block name. Example: block empty_block {}")
        }
        val blockName = token.value
        val blockPos = SourcePos(
            line = token.line,
            column = token.col
        )

        val blockBodyAndInnerBlocks = parseBlockBody(innerBlocksAllowed = true)

        return VnNode.Block(
            id = null,
            assignedId = null,
            name = blockName,
            body = blockBodyAndInnerBlocks.first,
            blocks = blockBodyAndInnerBlocks.second,
            pos = blockPos
        )
    }

    private fun parseBlockBody(
        innerBlocksAllowed: Boolean = false
    ): Pair<List<VnNode>, MutableMap<String, VnNode.Block>> {
        val innerBlocks: MutableMap<String, VnNode.Block> = mutableMapOf()

        if (peek() !is Token.OpenBraces) {
            throw RuntimeException("Expected \"{\"")
        }
        advance()

        val body = mutableListOf<VnNode>()

        var token = peek()
        while (token !is Token.CloseBraces) {
            if (token == null || token is Token.EOF) {
                throw RuntimeException("Every block must end with " +
                        "a close bracket. Example: block empty_block {}")
            }

            // Random numbers that don't fit anywhere become ignored statements for later warning
            if (token is Token.NumberLiteral) {
                body.add(
                    VnNode.IgnoredStatement(
                    value = token.value.toString(),
                    pos = SourcePos(
                        line = token.line,
                        column = token.col
                    )
                ))
                advance()
                token = peek()
                continue
            }

            // Random booleans that don't fit anywhere become ignored statements for later warning
            if (token is Token.BooleanLiteral) {
                body.add(
                    VnNode.IgnoredStatement(
                    value = token.value.toString(),
                    pos = SourcePos(
                        line = token.line,
                        column = token.col
                    )
                ))
                advance()
                token = peek()
                continue
            }

            val node = when (token) {
                is Token.Keyword -> parseKeyword()
                is Token.StringLiteral -> parseDialogue()
                is Token.Identifier -> parseIdentifier()
                is Token.OpenBraces -> throw RuntimeException("Unexpected open bracket encountered")
                Token.EOF -> throw RuntimeException("Unexpected end of file encountered")
                else -> throw RuntimeException("Unexpected token encountered: $token")
            }

            if (node is VnNode.Block) {
                if (innerBlocksAllowed) {
                    if (innerBlocks.containsKey(node.name)) {
                        throw RuntimeException("Duplicate block: ${node.name}")
                    }
                    innerBlocks.put(
                        key = node.name,
                        value = node
                    )
                } else {
                    throw RuntimeException("Blocks can't be defined inside conditionals")
                }
            } else {
                body.add(node)
            }

            token = peek()
        }

        advance()
        return Pair(body, innerBlocks)
    }

    private fun parseChoice(): VnNode.ChoiceStatement {
        val choices: MutableList<VnNode.ChoiceOption> = mutableListOf()

        val openBracket = advance()
        if (openBracket !is Token.OpenBraces) {
            throw RuntimeException("Choice keyword must be followed by an open bracket")
        }

        while (peek() is Token.StringLiteral) {
            val label = advance()
            var expression: Expression? = null
            if (label !is Token.StringLiteral) {
                throw RuntimeException("Every choice option must start with a StringLiteral")
            }

            if (peek() !is Token.OpenBraces) {
                try {
                    expression = parseExpression()
                } catch (_: RuntimeException) {
                    throw RuntimeException("Incorrect expression in choice block")
                }
            }

            val body = parseBlockBody()

            choices.add(
                VnNode.ChoiceOption(
                id = null,
                assignedId = null,
                expression = expression,
                label = label.value,
                body = body.first,
                pos = SourcePos(line = label.line, column = label.col)
            ))
        }

        val closeBracket = advance()
        if (closeBracket !is Token.CloseBraces) {
            throw RuntimeException("Choice keyword must end with a close bracket")
        }

        return VnNode.ChoiceStatement(
            options = choices,
            pos = choices.first().pos
        )
    }

    private fun parseExecute(): VnNode.ExecuteStatement {
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
                blockIndex = null,
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
                    blockIndex = null,
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