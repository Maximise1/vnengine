import com.maximise.vnengine.engine.lexer.Lexer
import java.io.File

fun main() {
    val lexer = Lexer()
    val file = File("/home/smol/project/VNEngine/res/script_example.vn")
    val tokens = lexer.tokenize(file.readText())
    tokens.forEach { token ->
        println(token)
    }
}
