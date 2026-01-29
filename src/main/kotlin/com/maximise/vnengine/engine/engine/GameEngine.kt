package com.maximise.vnengine.engine.engine

import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.ast.asBool
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.persistence.SaveHandler
import com.maximise.vnengine.engine.runtime.ExecutionState
import com.maximise.vnengine.engine.runtime.Interpreter
import kotlinx.coroutines.channels.Channel
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

class GameEngine(
    private val interpreter: Interpreter,
    private val saveHandler: SaveHandler,
    private val persistentDataHandler: PersistentDataHandler,
    private val program: VnNode.Program
) {
    private val stateListeners = mutableListOf<(GameState) -> Unit>()
    private val lua: Globals = JsePlatform.standardGlobals()
    private val screenHolder: MutableMap<String, String> = mutableMapOf()
    private val uiEventChannel: Channel<String> = Channel(Channel.UNLIMITED) // TODO: change type for smth else

    init {
        setupLuaAPI()
        loadLuaScripts()
        loadScreens()
    }

    fun addStateListener(listener: (GameState) -> Unit) {
        stateListeners.add(listener)
    }

    private fun loadScreens(path: String = "/home/smol/project/VNEngine/res/screens") {
        val dir = File(path)
        if (dir.isFile) {
            if (dir.extension == "html") {
                screenHolder.put(dir.name, dir.path)
                //println("screen ${dir.name} = ${dir.path}")
            }
        } else {
            dir.list().forEach { name ->
                loadScreens("$path/$name")
            }
        }
    }

    fun resolveScreen(name: String): String {
        return screenHolder[name] ?: throw RuntimeException("Screen $name not found")
    }

    private fun setupLuaAPI() {
        // show_screen(screen_name, data)
        lua.set("show_screen", object : TwoArgFunction() {
            override fun call(screenName: LuaValue, luaData: LuaValue): LuaValue {
                val screen = screenName.tojstring()
                val data = luaTableToMap(luaData)
                notifyListeners(GameState.ShowScreen(
                    screenName = screen,
                    data = data
                ))
                return NIL
            }
        })

        // start_game(saveName or null for new game)
        lua.set("start_game", object : OneArgFunction() {
            override fun call(saveName: LuaValue): LuaValue {
                val save = saveName.tojstring()
                runGame(save)
                return NIL
            }
        })

        lua.set("start_game", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                runGame()
                return NIL
            }
        })

        lua.set("advance_dialogue", object : ZeroArgFunction() {
            override fun call(): LuaValue {

                return NIL
            }
        })
    }

    private fun loadLuaScripts(path: String = "/home/smol/project/VNEngine/res/scripts") {
        val dir = File(path)
        if (dir.isFile) {
            if (dir.extension == "lua") {
                val script = dir.inputStream().readBytes().toString(Charsets.UTF_8)
                lua.load(script).call()
            }
        } else {
            dir.list().forEach { name ->
                loadLuaScripts("$path/$name")
            }
        }
    }

    fun callLuaFunction(functionName: String, args: List<String>) {
        try {
            val luaFunc = lua.get(functionName)

            if (luaFunc.isnil()) {
                println("Lua function not found: $functionName")
                return
            }

            when (args.size) {
                0 -> luaFunc.call()
                1 -> luaFunc.call(LuaValue.valueOf(args[0]))
                2 -> luaFunc.call(
                    LuaValue.valueOf(args[0]),
                    LuaValue.valueOf(args[1])
                )
                else -> {
                    val luaArgs = args.map { LuaValue.valueOf(it) }.toTypedArray()
                    luaFunc.invoke(luaArgs)
                }
            }
        } catch (e: Exception) {
            println("Error calling Lua function $functionName: ${e.message}")
            e.printStackTrace()
        }
    }

    fun start() {
        println("[Engine] Calling show_main_menu lua function")
        callLuaFunction("show_main_menu", emptyList())
    }

    fun showScreen(screenName: String, data: Map<String, Any>) {
        notifyListeners(GameState.ShowScreen(
            screenName = screenName,
            data = data
        ))
    }

    fun runGame(saveName: String? = null) {
        val (stack, vars) = if (saveName != null) {
            saveHandler.loadSave(saveName)
        } else {
            Pair(listOf(), mutableMapOf())
        }

        interpreter.run(
            program = program,
            persistentDialogue = persistentDataHandler.getSeenDialogue(),
            savedStack = stack,
            savedVariables = vars,
            persistentValues = persistentDataHandler.getVariables()
        )

        while (true) {
            when(val state = interpreter.advance()) {
                is ExecutionState.ShowDialogue -> {
                    notifyListeners(GameState.Dialogue(
                        speaker = state.dialogue.speaker,
                        text = state.dialogue.text,
                        isSeen = interpreter.context.isDialogueSeen(
                            state.dialogue.blockIndex!!
                        )
                    ))
                }

                is ExecutionState.ShowChoice -> {
                    val availableOptions = state.choiceStatement.options
                        .mapIndexedNotNull { index, option ->
                            if (option.expression == null ||
                                interpreter.evaluateExpression(option.expression).asBool()) {
                                ChoiceOption(index, option.label)
                            } else null
                        }

                    notifyListeners(GameState.Choice(availableOptions))
                }

                is ExecutionState.Finished -> {
                    notifyListeners(GameState.Finished)
                    break
                }
            }
        }
    }

    fun selectChoice(choiceIndex: Int) {
        interpreter.selectChoice(choiceIndex)
    }

    fun save(name: String? = null) {
        saveHandler.makeSave(
            name = name,
            stack = interpreter.context.stack,
            variables = interpreter.context.variables
        )
    }

    fun listSaves(): List<String> = saveHandler.listSaves()

    private fun notifyListeners(state: GameState) {
        stateListeners.forEach { it(state) }
    }

    private fun luaTableToMap(table: LuaValue): Map<String, Any> {
        if (!table.istable()) return emptyMap()

        val result = mutableMapOf<String, Any>()
        var key = LuaValue.NIL

        while (true) {
            val next = table.next(key)
            if (next.arg1().isnil()) break

            key = next.arg1()
            val value = next.arg(2)

            val keyStr = if (key.isint()) key.toint().toString() else key.tojstring()

            result[keyStr] = when {
                value.isint() -> value.toint()
                value.isnumber() -> value.todouble()
                value.isstring() -> value.tojstring()
                value.isboolean() -> value.toboolean()
                value.istable() -> luaTableToMap(value)
                else -> value.tojstring()
            }
        }

        return result
    }

    private fun mapToLuaTable(map: Map<String, Any>): LuaValue {
        val table = LuaValue.tableOf()

        map.forEach { (key, value) ->
            val luaValue = when (value) {
                is Int -> LuaValue.valueOf(value)
                is Double -> LuaValue.valueOf(value)
                is String -> LuaValue.valueOf(value)
                is Boolean -> LuaValue.valueOf(value)
                is Map<*, *> -> mapToLuaTable(value as Map<String, Any>)
                else -> LuaValue.valueOf(value.toString())
            }
            table.set(key, luaValue)
        }

        return table
    }
}