import com.maximise.vnengine.engine.runtime.Interpreter
import com.maximise.vnengine.engine.lexer.Lexer
import com.maximise.vnengine.engine.parser.Parser
import com.maximise.vnengine.engine.ui.ConsoleInterface
import java.io.File

fun main() {
    val lexer = Lexer()
    val file = File("/home/smol/project/VNEngine/res/script_example.vn")
    val tokens = lexer.tokenize(file.readText())

    //println(tokens)

    val parser = Parser()
    val ast = parser.parseProgram(tokens)

    /*for (block in ast.blocks) {
        for (node in block.value.body) {
            println(node)
        }
    }*/

    val console = ConsoleInterface()
    console.run(ast)
}
