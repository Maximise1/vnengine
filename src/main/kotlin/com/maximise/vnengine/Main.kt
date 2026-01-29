import com.maximise.vnengine.engine.di.Container
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities

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

    SwingUtilities.invokeLater {
        val client = cefApp.createClient()

        val browser = client.createBrowser(
            File(container.gameEngine.resolveScreen("base.html")).toURI().toString(),
            //"chrome:gpu",
            false,
            false
        )

        container.gui.initialize(
            browser,
            client
        )

        val frame = JFrame("smolVN").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            layout = BorderLayout()
            add(browser.uiComponent, BorderLayout.CENTER)
            size = Dimension(1280, 720)
            setLocationRelativeTo(null)
            isVisible = true
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            browser.close(true)
            client.dispose()
            cefApp.dispose()
        })
    }
}
