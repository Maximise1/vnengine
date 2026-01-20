import com.maximise.vnengine.engine.di.Container
import java.io.File

fun main() {
    val container = Container()

    val file = File("/home/smol/project/VNEngine/res/script_example.vn")
    val tokens = container.lexer.tokenize(file.readText())

    val ast = container.parser.parseProgram(tokens)

    container.consoleUI.run(ast, null)
}
