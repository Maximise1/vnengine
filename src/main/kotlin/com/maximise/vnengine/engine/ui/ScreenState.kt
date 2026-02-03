package com.maximise.vnengine.engine.ui

data class ScreenState (
    val params: MutableMap<String, Any> = mutableMapOf(),
    var screen: String
)