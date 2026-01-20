package com.maximise.vnengine.engine.ui

import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.ast.asBool
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.persistence.SaveHandler
import com.maximise.vnengine.engine.runtime.ExecutionState
import com.maximise.vnengine.engine.runtime.Interpreter

class ConsoleInterface(
    val interpreter: Interpreter,
    val saveHandler: SaveHandler,
    val persistentDataHandler: PersistentDataHandler
) {

    fun run(
        program: VnNode.Program,
        saveName: String?
    ) {
        val (stack, vars) = if (saveName != null) {
            saveHandler.loadSave(saveName)
        } else {
            Pair(listOf(), mutableMapOf())
        }


        interpreter.run(
            program = program,
            persistentDialogue = persistentDataHandler.getSeenDialogue(),
            savedStack = stack,
            savedVariables = vars,
            persistentValues = persistentDataHandler.getVariables()
        )
        gameLoop()
    }

    private fun gameLoop() {
        while (true) {
            when (val state = interpreter.advance()) {
                is ExecutionState.ShowDialogue -> {
                    displayDialogue(state.dialogue)
                    waitForInput()
                }
                is ExecutionState.ShowChoice -> {
                    val choice = getUserChoice(state.choiceStatement)
                    interpreter.selectChoice(choice)
                }
                is ExecutionState.Finished -> {
                    println("The end.")
                    break
                }
            }
        }
    }

    private fun displayDialogue(dialogue: VnNode.Dialogue) {
        val speaker = dialogue.speaker ?: ""
        val isSeen = interpreter.context.isDialogueSeen(dialogue.blockIndex!!)

        val prefix = if (isSeen) "[SEEN] " else ""
        println("$prefix$speaker: ${dialogue.text}")
    }

    private fun waitForInput() {
        readln()
    }

    private fun getUserChoice(choice: VnNode.ChoiceStatement): Int {
        choice.options.forEachIndexed { index, option ->
            if (option.expression == null ||
                interpreter.evaluateExpression(option.expression).asBool()) {
                println("$index: ${option.label}")
            }
        }

        while (true) {
            try {
                print("Choose: ")
                val input = readLine()?.toInt() ?: continue
                if (input >= 0 && input < choice.options.size) {
                    return input
                }
            } catch (e: Exception) {
                println("Invalid choice. Try again.")
            }
        }
    }
}