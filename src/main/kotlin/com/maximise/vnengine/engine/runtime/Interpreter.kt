package com.maximise.vnengine.engine.runtime

import com.maximise.vnengine.engine.ast.Expression
import com.maximise.vnengine.engine.ast.Value
import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.ast.asBool
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.runtime.ExecutionContext

class Interpreter() {

    var context = ExecutionContext(
        blocks = mutableMapOf(),
        dialogue = mutableMapOf(),
        savedStack = ArrayDeque(),
        savedVariables = mutableMapOf(),
        persistentVals = mutableMapOf(),
    )

    private var currentDialogue: VnNode.Dialogue? = null
    private var currentChoice: VnNode.ChoiceStatement? = null
    private var isFinished = false

    fun run(
        program: VnNode.Program,
        persistentDialogue: MutableMap<String, Short>,
        savedStack: List<Pair<String, Int>>,
        savedVariables: MutableMap<String, Value>,
        persistentValues: MutableMap<String, Value>
    ) {
        context = ExecutionContext(
            blocks = program.blocks,
            dialogue = persistentDialogue,
            savedStack = savedStack,
            savedVariables = savedVariables,
            persistentVals = persistentValues,
        )

        if (savedStack.isEmpty()) {
            val startBlock = context.findBlock("start")
                ?: throw RuntimeException("Program must contain \"start\" block")
            context.pushBlock(startBlock)
        }
    }

    fun advance(): ExecutionState {
        if (isFinished) return ExecutionState.Finished

        if (context.stack.isEmpty()) {
            isFinished = true
            return ExecutionState.Finished
        }

        val node = context.currentBlock()!!.body[context.currentIndex()!!]

        return when (node) {
            is VnNode.Dialogue -> {
                currentDialogue = node
                context.incrementIndex()
                ExecutionState.ShowDialogue(node)
            }
            is VnNode.ExecuteStatement -> {
                executeBlock(node.targetBlock)
                advance()
            }
            is VnNode.IgnoredStatement -> {
                executeIgnoredStatement(node)
                advance()
            }
            is VnNode.AssignStatement -> {
                executeAssignStatement(node)
                advance()
            }
            is VnNode.ChoiceStatement -> {
                currentChoice = node
                ExecutionState.ShowChoice(node)
            }
            is VnNode.IfStatement -> {
                executeIfStatement(node)
                advance()
            }
            else -> throw RuntimeException("Unexpected VnNode encountered: $node")
        }
    }

    fun selectChoice(index: Int) {
        val choice = currentChoice
            ?: throw IllegalStateException("No active choice")

        if (index < 0 || index >= choice.options.size) {
            throw IllegalArgumentException("Invalid choice index")
        }

        context.pushBlock(
            block = VnNode.Block(
                name = "",
                body = choice.options[index].body,
                blocks = mapOf(),
                pos = choice.options[index].pos,
                id = choice.options[index].id,
                assignedId = choice.options[index].assignedId
            ),
            index = 0
        )

        currentChoice = null
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

    fun evaluateExpression(expression: Expression): Value {
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
                variable
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
                id = choiceStatement.options[index].id,
                assignedId = choiceStatement.options[index].assignedId
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
                        pos = ifStatement.pos,
                        assignedId = ifStatement.branches[i].assignedId
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
                    pos = ifStatement.pos,
                    assignedId = ifStatement.elseBody.assignedId
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