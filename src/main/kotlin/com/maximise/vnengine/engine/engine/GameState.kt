package com.maximise.vnengine.engine.engine

import com.maximise.vnengine.engine.ast.VnNode

sealed class GameState {
    data class Dialogue(
        val speaker: String?,
        val text: String,
        val isSeen: Boolean
    ) : GameState()

    data class Choice(
        val options: List<ChoiceOption>
    ) : GameState()

    object Finished : GameState()
}

data class ChoiceOption(
    val index: Int,
    val label: String
)