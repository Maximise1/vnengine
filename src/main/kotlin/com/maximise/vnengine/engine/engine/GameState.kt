package com.maximise.vnengine.engine.engine

import com.maximise.vnengine.engine.ast.AdvanceMode
import com.maximise.vnengine.engine.ast.PositionMode
import com.maximise.vnengine.engine.ast.PositionValue

sealed class GameState {
    data class ShowScreen(
        val screenName: String,
        val data: Map<String, Any>,
    ) : GameState()

    object PopScreen : GameState()

    data class Dialogue(
        val speaker: String?,
        val text: String,
        val isSeen: Boolean
    ) : GameState()

    data class Choice(
        val options: List<ChoiceOption>
    ) : GameState()

    data class Background(
        val image: String,
        val advanceMode: AdvanceMode,
        val positionMode: PositionMode,
        val time: Double?,
        val startx: PositionValue?,
        val starty: PositionValue?,
        val endx: PositionValue?,
        val endy: PositionValue?,
        val x: PositionValue?,
        val y: PositionValue?
    ) : GameState()

    object Finished : GameState()
}

data class ChoiceOption(
    val index: Int,
    val label: String
)