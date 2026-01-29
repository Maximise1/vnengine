package com.maximise.vnengine.engine.di

import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.engine.GameEngine
import com.maximise.vnengine.engine.lexer.Lexer
import com.maximise.vnengine.engine.parser.Indexer
import com.maximise.vnengine.engine.parser.Parser
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.persistence.SaveHandler
import com.maximise.vnengine.engine.runtime.Interpreter
import com.maximise.vnengine.engine.ui.GUI
import java.io.File

class Container {
    val persistentDataHandler: PersistentDataHandler by lazy {
        PersistentDataHandler()
    }

    val saveHandler: SaveHandler by lazy {
        SaveHandler()
    }

    val indexer: Indexer by lazy {
        Indexer()
    }

    val interpreter: Interpreter by lazy {
        Interpreter()
    }

    val parser: Parser by lazy {
        Parser(indexer)
    }

    val lexer: Lexer by lazy {
        Lexer()
    }

    val gameEngine: GameEngine by lazy {
        GameEngine(
            interpreter = interpreter,
            saveHandler = saveHandler,
            persistentDataHandler = persistentDataHandler,
            program = parseProgram()
        )
    }

    val gui: GUI by lazy {
        GUI(gameEngine)
    }

    fun parseProgram(): VnNode.Program {
        val file = File("/home/smol/project/VNEngine/res/script_example.vn")
        val tokens = lexer.tokenize(file.readText())
        return parser.parseProgram(tokens)
    }
}