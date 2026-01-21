package com.maximise.vnengine.engine.ui

import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.engine.GameEngine
import com.maximise.vnengine.engine.engine.GameState

class ConsoleInterface(
    private val gameEngine: GameEngine
) {

    fun run(program: VnNode.Program, saveName: String? = null) {
        gameEngine.addStateListener { state ->
            handleState(state)
        }

        gameEngine.start(program, saveName)
    }

    private fun handleState(state: GameState) {
        when (state) {
            is GameState.Dialogue -> {
                displayDialogue(state)
                waitForInput()
            }
            is GameState.Choice -> {
                val choice = getUserChoice(state)
                gameEngine.selectChoice(choice)
            }
            is GameState.Finished -> {
                println("The end.")
            }
        }
    }

    private fun displayDialogue(dialogue: GameState.Dialogue) {
        val speaker = dialogue.speaker ?: ""
        val prefix = if (dialogue.isSeen) "[SEEN] " else ""
        println("$prefix$speaker: ${dialogue.text}")
    }

    private fun waitForInput() {
        while (true) {
            val command = readln().split(" ")
            when (command[0]) {
                "" -> return
                "save" -> gameEngine.save()
                "list" -> gameEngine.listSaves().forEach { println(it) }
                "load" -> {
                    gameEngine.load(command[1])
                    return
                }
            }
        }
    }

    private fun getUserChoice(choice: GameState.Choice): Int {
        choice.options.forEach { option ->
            println("${option.index}: ${option.label}")
        }

        while (true) {
            try {
                print("Choose: ")
                val input = readLine()?.toInt() ?: continue
                if (choice.options.any { it.index == input }) {
                    return input
                }
            } catch (e: Exception) {
                println("Invalid choice. Try again.")
            }
        }
    }
}