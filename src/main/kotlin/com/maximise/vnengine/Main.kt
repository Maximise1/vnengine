import com.maximise.vnengine.engine.di.Container
import io.github.oshai.kotlinlogging.KotlinLogging
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.handler.CefKeyboardHandler
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities

val logger = KotlinLogging.logger {  }

fun main(args: Array<String>) {
    CefApp.startup(args)

    val builder = CefAppBuilder()

    builder.addJcefArgs(*args)
    builder.addJcefArgs(
        "--no-sandbox",
        "--disable-dev-shm-usage"
    )
    builder.cefSettings.apply {
        windowless_rendering_enabled = false
        remote_debugging_port = 8088
        log_severity = CefSettings.LogSeverity.LOGSEVERITY_INFO
    }

    val cefApp = builder.build()
    val container = Container()

    var isFullscreen = true

    SwingUtilities.invokeLater {
        val client = cefApp.createClient()

        val device: GraphicsDevice =
            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice

        val browser = client.createBrowser(
            File(container.assetLoader.resolveScreen("base.html")).toURI().toString(),
            //"chrome:gpu",
            false,
            false
        )

        container.gui.initialize(
            browser,
            client
        )

        val frame = createBrowserFrame(device, browser)

        client.addKeyboardHandler(object : org.cef.handler.CefKeyboardHandlerAdapter() {
            override fun onKeyEvent(
                browser: CefBrowser?,
                event: CefKeyboardHandler.CefKeyEvent?
            ): Boolean {
                if (event?.type == CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN &&
                    event.windows_key_code == KeyEvent.VK_ESCAPE
                ) {
                    SwingUtilities.invokeLater {
                        logger.debug { "ESC detected via CEF" }
                        if (isFullscreen) {
                            exitFullscreen(device, frame)
                            isFullscreen = false
                        } else {
                            enterFullscreen(device, frame)
                            isFullscreen = true
                        }
                    }
                    return true // mark handled
                }
                return false
            }
        })

        Runtime.getRuntime().addShutdownHook(Thread {
            browser.close(true)
            client.dispose()
            cefApp.dispose()
            container.gameEngine.shutdown() // TODO: maybe move it to a proper place?
        })
    }
}

fun createBrowserFrame(device: GraphicsDevice, browser: CefBrowser): JFrame {
    // Create the window
    val frame = JFrame("smolVN").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        layout = BorderLayout()

        // Add the JCEF browser UI into the center of the frame
        add(browser.uiComponent, BorderLayout.CENTER)

        // Start windowed first (required before switching modes)
        size = Dimension(1280, 720)
        setLocationRelativeTo(null)
        isVisible = true
    }

    // Optional: press ESC to leave fullscreen
    frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            logger.debug { "Event caught: $e" }
            if (e.keyCode == KeyEvent.VK_ESCAPE) {
                exitFullscreen(device, frame)
            }
        }
    })

    // Example: go fullscreen immediately
    enterFullscreen(device, frame)

    return frame
}

/**
 * Restores the window back to normal windowed mode.
 */
fun exitFullscreen(device: GraphicsDevice, frame: JFrame) {
    device.fullScreenWindow = null   // Release fullscreen
    frame.dispose()
    frame.isUndecorated = false      // Restore window borders
    frame.isResizable = true
    frame.size = Dimension(1280, 720)
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
}

/**
 * Switches the window into true exclusive fullscreen.
 * Removes window borders and lets the OS treat this as a fullscreen app.
 */
fun enterFullscreen(device: GraphicsDevice, frame: JFrame) {
    frame.dispose()                  // Must dispose before changing decoration
    frame.isUndecorated = true       // Remove title bar & borders
    frame.isResizable = false
    device.fullScreenWindow = frame  // OS-level fullscreen takeover
    frame.isVisible = true
}