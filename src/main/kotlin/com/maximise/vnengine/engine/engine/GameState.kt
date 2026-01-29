package com.maximise.vnengine.engine.engine

sealed class GameState {
    data class ShowScreen(
        val screenName: String,
        val data: Map<String, Any>
    ) : GameState()

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