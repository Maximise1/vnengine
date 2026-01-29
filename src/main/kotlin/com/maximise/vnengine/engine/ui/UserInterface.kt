package com.maximise.vnengine.engine.ui

import com.maximise.vnengine.engine.engine.GameState
import org.cef.CefClient
import org.cef.browser.CefBrowser

interface UserInterface {
    fun handleState(state: GameState)
    fun initialize(
        browser: CefBrowser,
        client: CefClient
    )
}