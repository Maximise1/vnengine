package com.maximise.vnengine.engine.interpreter

import com.maximise.vnengine.engine.parser.Expression
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
        val node = context.currentBlock()!!.body[context.currentIndex()!!]

        when (node) {
            is VnNode.Dialogue -> executeDialogue(node)
            is VnNode.ExecuteStatement -> executeBlock(node.targetBlock)
            is VnNode.IgnoredStatement -> executeIgnoredStatement(node)
            is VnNode.AssignStatement -> executeAssignStatement(node)
            is VnNode.ChoiceStatement -> executeChoiceStatement(node)
            is VnNode.IfStatement -> executeIfStatement(node)
            else -> throw RuntimeException("Unexpected VnNode encountered: $node")
        }
    }

    // All variables are global, type is not enforced
    private fun executeAssignStatement(assignStatement: VnNode.AssignStatement) {
        when (assignStatement.value) {
            is Expression.StringLiteral -> {
                context.setVariable(
                    assignStatement.variable,
                    assignStatement.value.value
                )
            }
            is Expression.NumberLiteral -> {
                context.setVariable(
                    assignStatement.variable,
                    assignStatement.value.value
                )
            }
            is Expression.BooleanLiteral -> {
                context.setVariable(
                    assignStatement.variable,
                    assignStatement.value.value
                )
            }
            else -> throw RuntimeException(
                "Interpreter error: Unexpected Expression passed as Literal")
        }

        context.incrementIndex()
    }

    private fun evaluateExpression(expression: Expression): Value {
        val result = when (expression) {
            is Expression.BinaryOperator -> {
                val leftValue = evaluateExpression(expression.left)
                val rightValue = evaluateExpression(expression.right)
                expression.operator.apply(leftValue, rightValue)
            }
            is Expression.UnaryOperator -> {
                val operand = evaluateExpression(expression.expression)
                expression.operator.apply(operand)
            }
            is Expression.NumberLiteral -> Value.Num(expression.value)
            is Expression.StringLiteral -> Value.Str(expression.value)
            is Expression.BooleanLiteral -> Value.Bool(expression.value)
            is Expression.Variable -> {
                val variable = context.getVariable(expression.name)
                if (variable == null) {
                    throw RuntimeException("Undefined variable ${expression.name} was used")
                }
                variable.value
            }
        }
        return result
    }

    private fun executeChoiceStatement(choiceStatement: VnNode.ChoiceStatement) {
        choiceStatement.options.forEachIndexed { index, option ->
            if (option.expression == null ||
                evaluateExpression(option.expression).asBool()) {
                println("$index: ${option.label}")
            }
        }

        var index: Int? = null

        while (index == null) {
            try {
                index = readLine()?.toInt()
                if (index!! < 0 || index > choiceStatement.options.size) {
                    throw RuntimeException("")
                }
            } catch (e: Exception) {
                println("Incorrect value passed. Try again.")
            }
        }

        context.pushBlock(
            block = VnNode.Block(
                name = "",
                body = choiceStatement.options[index].body,
                blocks = mapOf(),
                pos = choiceStatement.options[index].pos,
                id = choiceStatement.options[index].id
            ),
            index = 0
        )
    }

    private fun executeIfStatement(ifStatement: VnNode.IfStatement) {
        var smthExecuted = false

        for (i in 0..(ifStatement.branches.size - 1)) {
            if (evaluateExpression(ifStatement.branches[i].condition).asBool()) {
                smthExecuted = true
                context.pushBlock(
                    block = VnNode.Block(
                        id = ifStatement.branches[i].id,
                        name = "",
                        body = ifStatement.branches[i].body,
                        blocks = mapOf(),
                        pos = ifStatement.pos
                    ),
                    index = 0
                )
                break
            }
        }

        if (!smthExecuted && ifStatement.elseBody != null) {
            context.pushBlock(
                block = VnNode.Block(
                    id = ifStatement.elseBody.id,
                    name = "",
                    body = ifStatement.elseBody.body,
                    blocks = mapOf(),
                    pos = ifStatement.pos
                ),
                index = 0
            )
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