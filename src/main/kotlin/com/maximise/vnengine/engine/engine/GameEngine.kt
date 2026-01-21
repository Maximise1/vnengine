package com.maximise.vnengine.engine.engine

import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.ast.asBool
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.persistence.SaveHandler
import com.maximise.vnengine.engine.runtime.ExecutionState
import com.maximise.vnengine.engine.runtime.Interpreter

class GameEngine(
    private val interpreter: Interpreter,
    private val saveHandler: SaveHandler,
    private val persistentDataHandler: PersistentDataHandler
) {
    private var program: VnNode.Program? = null
    private val stateListeners = mutableListOf<(GameState) -> Unit>()

    fun addStateListener(listener: (GameState) -> Unit) {
        stateListeners.add(listener)
    }

    fun start(program: VnNode.Program, saveName: String? = null) {
        this.program = program

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
        runGame()
    }

    fun runGame() { // TODO: rename this function to advance and move game loop into a different function that will account for load/exit commands
        while (true) {
            when(val state = interpreter.advance()) {
                is ExecutionState.ShowDialogue -> {
                    notifyListeners(GameState.Dialogue(
                        speaker = state.dialogue.speaker,
                        text = state.dialogue.text,
                        isSeen = interpreter.context.isDialogueSeen(
                            state.dialogue.blockIndex!!
                        )
                    ))
                }

                is ExecutionState.ShowChoice -> {
                    val availableOptions = state.choiceStatement.options
                        .mapIndexedNotNull { index, option ->
                            if (option.expression == null ||
                                interpreter.evaluateExpression(option.expression).asBool()) {
                                ChoiceOption(index, option.label)
                            } else null
                        }

                    notifyListeners(GameState.Choice(availableOptions))
                }

                is ExecutionState.Finished -> {
                    notifyListeners(GameState.Finished)
                    break
                }
            }
        }
    }

    fun selectChoice(choiceIndex: Int) {
        interpreter.selectChoice(choiceIndex)
    }

    fun save(name: String? = null) {
        saveHandler.makeSave(
            name = name,
            stack = interpreter.context.stack,
            variables = interpreter.context.variables
        )
    }

    fun load(saveName: String) {
        val prog = program ?: throw IllegalStateException("No program loaded")
        val (stack, variables) = saveHandler.loadSave(saveName)

        interpreter.run(
            program = prog,
            persistentDialogue = persistentDataHandler.getSeenDialogue(),
            savedStack = stack,
            savedVariables = variables,
            persistentValues = persistentDataHandler.getVariables()
        )

        // TODO: update current dialogue, choice and bg
    }

    fun listSaves(): List<String> = saveHandler.listSaves()

    private fun notifyListeners(state: GameState) {
        stateListeners.forEach { it(state) }
    }
}