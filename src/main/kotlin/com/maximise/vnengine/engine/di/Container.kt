package com.maximise.vnengine.engine.di

import com.maximise.vnengine.engine.lexer.Lexer
import com.maximise.vnengine.engine.parser.Indexer
import com.maximise.vnengine.engine.parser.Parser
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.persistence.SaveHandler
import com.maximise.vnengine.engine.runtime.Interpreter
import com.maximise.vnengine.engine.ui.ConsoleInterface

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

    val consoleUI: ConsoleInterface by lazy {
        ConsoleInterface(
            interpreter,
            saveHandler,
            persistentDataHandler
        )
    }
}