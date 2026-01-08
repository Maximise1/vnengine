import com.maximise.vnengine.engine.lexer.Lexer
import com.maximise.vnengine.engine.parser.Parser
import java.io.File

fun main() {
    val lexer = Lexer()
    val file = File("/home/smol/project/VNEngine/res/script_example.vn")
    val tokens = lexer.tokenize(file.readText())
    val parser = Parser()
    val ast = parser.parseProgram(tokens)
    ast.blocks.forEach { (key, value) ->
        println("$key = $value")
        println()
    }
}
