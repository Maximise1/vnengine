package com.maximise.vnengine.engine.ui

import com.maximise.vnengine.engine.ast.PositionMode
import com.maximise.vnengine.engine.ast.PositionValue
import com.maximise.vnengine.engine.engine.AssetLoader
import com.maximise.vnengine.engine.engine.GameEngine
import com.maximise.vnengine.engine.engine.GameState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.luaj.vm2.ast.Str
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {  }

class GUI(
    private val gameEngine: GameEngine,
    private val assetLoader: AssetLoader
) : UserInterface {

    private val templateEngine = TemplateEngine()
    private lateinit var browser: CefBrowser
    private var engineStarted = false
    private val screenStack: ArrayDeque<ScreenState> = ArrayDeque()

    @Suppress("UNCHECKED_CAST")
    override fun handleState(state: GameState) {
        if (!::browser.isInitialized) {
            logger.warn { "Browser not initialized yet, skipping state: $state" }
            return
        }

        logger.debug { "Handling state: $state" }

        when (state) {
            is GameState.ShowScreen -> {
                screenStack.add(ScreenState(
                    screen = state.screenName,
                ))

                state.data.forEach { key, value ->
                    screenStack.last().params[key] = value
                }

                showScreen(screenStack.last().screen, screenStack.last().params)
            }

            is GameState.Dialogue -> {
                if (screenStack.last().params["choices"] != null) { // TODO: micromanaging state can lead to bugs and feels too strict to be here (what if user wants to show dialogue + choice menu?); potential refactor: maybe give lua more control and do all this through lua functions
                    screenStack.last().params.remove("choices")
                }
                if (state.speaker != null) screenStack.last().params["speaker"] = state.speaker
                screenStack.last().params["isSeen"] = state.isSeen
                screenStack.last().params["text"] = state.text

                showScreen(
                    screenStack.last().screen,
                    screenStack.last().params
                )
            }

            is GameState.Choice -> {
                if (screenStack.last().params["text"] != null) {
                    screenStack.last().params.remove("text")
                }
                if (screenStack.last().params["isSeen"] != null) {
                    screenStack.last().params.remove("isSeen")
                }
                if (screenStack.last().params["speaker"] != null) {
                    screenStack.last().params.remove("speaker")
                }

                screenStack.last().params["choices"] = state.options.map { option ->
                    mapOf(
                        "index" to option.index,
                        "label" to option.label
                    )
                }

                showScreen(
                    screenStack.last().screen,
                    screenStack.last().params
                )
            }

            is GameState.Background -> {
                val background = assetLoader.resolveImage(state.image)
                val animationDuration = state.time ?: 0.0
                var backgroundPosition: String? = null
                var startx: Double? = null
                var endx: Double? = null
                var endy: Double? = null
                var starty: Double? = null
                var x: Double? = null
                var y: Double? = null
                var backgroundAnimation: String? = null

                screenStack.last().params["background"] = background
                screenStack.last().params["animationDuration"] = animationDuration

                if (state.positionMode in listOf(
                        PositionMode.CENTER,
                        PositionMode.LEFT,
                        PositionMode.RIGHT,
                        PositionMode.TOP,
                        PositionMode.BOTTOM
                    )) {
                    backgroundPosition = state.positionMode.name.lowercase()
                } else if (state.positionMode == PositionMode.CUSTOM_ANIMATION) {
                    backgroundPosition = state.positionMode.name.lowercase()
                    val (imageX, imageY) = getImageDimensions(background)

                    startx = state.startx?.toPercent(imageX) ?: 50.0
                    starty = state.starty?.toPercent(imageY) ?: 50.0
                    endx = state.endx?.toPercent(imageX) ?: 50.0
                    endy = state.endy?.toPercent(imageY) ?: 50.0
                } else if (state.positionMode == PositionMode.CUSTOM_STATIC) {
                    backgroundPosition = state.positionMode.name.lowercase()
                    val (imageX, imageY) = getImageDimensions(background)

                    x = state.x?.toPercent(imageX) ?: 50.0
                    y = state.y?.toPercent(imageY) ?: 50.0
                } else {
                    val positionMode = state.positionMode
                        .name.lowercase().replace('_', '-')
                    backgroundAnimation = "bg-$positionMode"
                }

                screenStack.last().params["background"] = background
                screenStack.last().params["animationDuration"] = animationDuration
                screenStack.last().params["backgroundPosition"] = backgroundPosition ?: ""
                screenStack.last().params["startx"] = startx?.let { "$startx%" } ?: ""
                screenStack.last().params["endx"] = endx?.let { "$endx%" } ?: ""
                screenStack.last().params["starty"] = starty?.let { "$starty%" } ?: ""
                screenStack.last().params["endy"] = endy?.let { "$endy%" } ?: ""
                screenStack.last().params["y"] = y?.let { "$y%" } ?: ""
                screenStack.last().params["x"] = x?.let { "$x%" } ?: ""
                if (backgroundAnimation == null) {
                    screenStack.last().params.remove("backgroundAnimation")
                } else {
                    screenStack.last().params["backgroundAnimation"] = backgroundAnimation
                }

                screenStack.last().params.forEach { key, value ->
                    logger.debug { "parameter $key = $value" }
                }

                showScreen(
                    screenStack.last().screen,
                    screenStack.last().params
                )
            }

            is GameState.Finished -> {
                screenStack.removeLast()
                if (screenStack.isEmpty()) {
                    screenStack.add(ScreenState(
                        screen = "main_menu"
                    ))
                }

                showScreen(
                    screenStack.last().screen,
                    screenStack.last().params
                )
            }

            is GameState.PopScreen -> { // maybe remove all screen from the stack and leave user at main menu?
                screenStack.removeLast()
                if (screenStack.isEmpty()) {
                    screenStack.add(ScreenState(
                        screen = "main_menu"
                    ))
                }

                showScreen(
                    screenStack.last().screen,
                    screenStack.last().params
                )
            }
        }
    }

    /**
     * returns Pair(width, height)
     */
    private fun getImageDimensions(imagePath: String): Pair<Int, Int> {
        val file = File(imagePath)

        ImageIO.createImageInputStream(file).use { inputStream ->
            val readers = ImageIO.getImageReaders(inputStream)

            if (!readers.hasNext()) {
                throw IllegalArgumentException("Unsupported image format: $imagePath")
            }

            val reader = readers.next()
            reader.input = inputStream
            val result = Pair(reader.getWidth(0), reader.getHeight(0))

            reader.dispose()
            return result
        }
    }

    override fun initialize(
        browser: CefBrowser,
        client: CefClient
    ) {
        logger.info { "Starting initialization" }
        this.browser = browser

        setupMessageRouter(client)

        gameEngine.addStateListener { state ->
            handleState(state)
        }

        logger.info { "Initialization finished" }
        //gameEngine.start()
    }

    private fun setupMessageRouter(client: CefClient) {
        logger.info { "Setting up message router" }

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

                logger.info { "JS called Lua: $functionName with args: $args" }
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
                    logger.info { "Page loaded, starting game engine" }
                    gameEngine.start()
                }
            }
        })
    }

    private fun showScreen(screenName: String, data: Map<String, Any>) {
        logger.info { "Showing screen: $screenName" }

        val html = templateEngine.render(
            assetLoader.resolveScreen("$screenName.html"),
            data
        )

        val escapedHtml = html
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "")

        if (escapedHtml.length > 500_000) {
            logger.warn { "Very large HTML for screen $screenName" }
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