package com.maximise.vnengine.engine.engine

sealed class UiEvent {
    object AdvanceExecution : UiEvent()
    data class SelectChoice(val index: Int) : UiEvent()
    object ExitGameLoop : UiEvent()
}