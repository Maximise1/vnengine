package com.maximise.vnengine.engine.ui

import com.maximise.vnengine.engine.engine.GameEngine
import com.maximise.vnengine.engine.engine.GameState
import kotlinx.coroutines.channels.Channel
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter

class GUI(
    private val gameEngine: GameEngine
) : UserInterface {

    private val templateEngine = TemplateEngine()
    private lateinit var browser: CefBrowser
    private var engineStarted = false

    override fun handleState(state: GameState) {
        if (!::browser.isInitialized) {
            println("[GUI] Browser not initialized yet, skipping state: $state")
            return
        }

        when (state) {
            is GameState.ShowScreen -> {
                showScreen(state.screenName, state.data)
            }

            is GameState.Dialogue -> {
                /*showScreen(
                    "game_screen",
                    mapOf(
                        "speaker" to state.speaker,
                        "text" to state.text,
                        "isSeen" to state.isSeen
                    ) as Map<String, Any>
                )*/
            }

            is GameState.Choice -> {
                /*showScreen(
                    "game_screen",
                    state.options.associate { option ->
                        option.index.toString() to option.label
                    }
                )*/
            }

            is GameState.Finished -> {
                showScreen(
                    "main_menu",
                    emptyMap()
                )
            }
        }
    }

    override fun initialize(
        browser: CefBrowser,
        client: CefClient
    ) {
        println("[GUI] Starting initialization")
        this.browser = browser

        setupMessageRouter(client)

        gameEngine.addStateListener { state ->
            handleState(state)
        }

        println("[GUI] Initialization finished")
        //gameEngine.start()
    }

    private fun setupMessageRouter(client: CefClient) {
        println("[GUI] Setting up message router")

        val router = CefMessageRouter.create()

        router.addHandler(object : CefMessageRouterHandlerAdapter() {
            override fun onQuery(
                browser: CefBrowser,
                frame: CefFrame,
                queryId: Long,
                request: String,
                persistent: Boolean,
                callback: CefQueryCallback
            ): Boolean {
                val parts = request.split(":", limit = 2)
                val functionName = parts[0]
                val args = if (parts.size > 1) {
                    parts[1].split(",").filter { it.isNotEmpty() }
                } else {
                    emptyList()
                }

                println("[GUI] JS called Lua: $functionName with args: $args")
                gameEngine.callLuaFunction(functionName, args)

                callback.success("OK")
                return true
            }
        }, true)

        client.addMessageRouter(router)

        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain && !engineStarted) {
                    engineStarted = true
                    println("[GUI] Page loaded, starting game engine")
                    gameEngine.start()
                }
            }
        })
    }

    private fun showScreen(screenName: String, data: Map<String, Any>) {
        println("[GUI] Showing screen: $screenName")

        val html = templateEngine.render(
            gameEngine.resolveScreen("$screenName.html"),
            data
        )

        val escapedHtml = html
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "")

        if (escapedHtml.length > 500_000) {
            println("[GUI] WARNING: Very large HTML for screen $screenName")
        }

        browser.executeJavaScript("""
            (function() {
                var container = document.getElementById('screen-container');
                if (container) {
                    container.innerHTML = `$escapedHtml`;
                    console.log('Screen updated: $screenName');
                } else {
                    console.error('screen-container not found');
                }
            })();
        """,browser.url, 0)
    }
}