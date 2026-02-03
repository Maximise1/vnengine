package com.maximise.vnengine.engine.engine

import com.maximise.vnengine.engine.ast.AdvanceMode
import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.ast.asBool
import com.maximise.vnengine.engine.persistence.PersistentDataHandler
import com.maximise.vnengine.engine.persistence.SaveHandler
import com.maximise.vnengine.engine.runtime.ExecutionState
import com.maximise.vnengine.engine.runtime.Interpreter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

private val logger = KotlinLogging.logger {  }

class GameEngine(
    private val interpreter: Interpreter,
    private val saveHandler: SaveHandler,
    private val persistentDataHandler: PersistentDataHandler,
    private val program: VnNode.Program,
    private val assetLoader: AssetLoader
) {
    private val stateListeners = mutableListOf<(GameState) -> Unit>()
    private val lua: Globals = JsePlatform.standardGlobals()
    private var uiEventChannel: Channel<UiEvent>? = null
    private val engineScope: CoroutineScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    )
    private var currentGameJob: Job? = null
    private val screenState: MutableMap<String, Any> = mutableMapOf()

    init {
        setupLuaAPI()
        loadLuaScripts()
    }

    fun addStateListener(listener: (GameState) -> Unit) {
        stateListeners.add(listener)
    }

    private suspend fun waitForEventOrTimeout(seconds: Double): UiEvent {
        return withTimeoutOrNull((seconds * 1000).toLong()) {
            uiEventChannel!!.receive()
        } ?: UiEvent.AdvanceExecution
    }

    private fun startGame(saveName: String? = null) {
        currentGameJob?.cancel()
        uiEventChannel?.cancel()

        uiEventChannel = Channel(Channel.UNLIMITED)

        currentGameJob = engineScope.launch {
            try {
                runGame(saveName)
            } catch (e: CancellationException) {
                logger.info { "Game cancelled" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to start new game" }
            }
        }
    }

    fun sendUiEvent(event: UiEvent) {
        logger.debug { "Sending event: $event" }
        uiEventChannel!!.trySend(event)
            .onFailure {
                logger.warn { "Failed to send UI event: $event" }
            }
    }

    private fun setupLuaAPI() {
        // show_screen(screen_name, data)
        lua.set("show_screen", object : TwoArgFunction() {
            override fun call(screenName: LuaValue, luaData: LuaValue): LuaValue {
                val screen = screenName.tojstring()
                val data = luaTableToMap(luaData)
                showScreen(screen, data)
                return NIL
            }
        })

        // start_game(saveName or null for new game)
        lua.set("load_game", object : OneArgFunction() {
            override fun call(saveName: LuaValue): LuaValue {
                val save = saveName.tojstring()
                startGame(save)
                return NIL
            }
        })

        lua.set("start_game", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                startGame()
                return NIL
            }
        })

        lua.set("advance_dialogue", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                sendUiEvent(UiEvent.AdvanceExecution)
                //engineScope.launch {
                //    uiEventChannel.send(UiEvent.AdvanceDialogue)
                //}
                return NIL
            }
        })

        lua.set("select_choice", object : OneArgFunction() {
            override fun call(index: LuaValue): LuaValue {
                val choiceIndex = index.toint()
                logger.debug { "Choice selected: $choiceIndex" }
                sendUiEvent(UiEvent.SelectChoice(choiceIndex))
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
                logger.warn { "Lua function not found: $functionName" }
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
            logger.error { "Error calling Lua function $functionName: ${e.message}" }
        }
    }

    fun start() {
        logger.debug { "Calling show_main_menu lua function" }
        callLuaFunction("show_main_menu", emptyList())
    }

    fun showScreen(screenName: String, data: Map<String, Any>) {
        notifyListeners(GameState.ShowScreen(
            screenName = screenName,
            data = data
        ))
    }

    suspend fun runGame(saveName: String? = null) {
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
            val state = advance()
            val event = when {
                state is GameState.Background && state.advanceMode == AdvanceMode.AUTO -> {
                    waitForEventOrTimeout(state.time ?: 0.0)
                }
                else -> {
                    uiEventChannel!!.receive()
                }
            }

            logger.debug { "Event received: $event" }

            if (event is UiEvent.ExitGameLoop) break
            handleEvent(event)
        }
    }

    private fun advance(): GameState {
        return when(val state = interpreter.advance()) {
            is ExecutionState.ShowDialogue -> {
                val state = GameState.Dialogue(
                    speaker = state.dialogue.speaker,
                    text = state.dialogue.text,
                    isSeen = interpreter.context.isDialogueSeen(
                        state.dialogue.blockIndex!!
                    )
                )
                notifyListeners(state)
                state
            }

            is ExecutionState.ShowChoice -> {
                val availableOptions = state.choiceStatement.options
                    .mapIndexedNotNull { index, option ->
                        if (option.expression == null ||
                            interpreter.evaluateExpression(option.expression).asBool()) {
                            ChoiceOption(index, option.label)
                        } else null
                    }
                val state = GameState.Choice(availableOptions)

                notifyListeners(state)
                state
            }

            is ExecutionState.ShowBackground -> {
                val state = GameState.Background(
                    image = state.backgroundNode.image,
                    advanceMode = state.backgroundNode.advance,
                    positionMode = state.backgroundNode.positionMode,
                    time = state.backgroundNode.time,
                    startx = state.backgroundNode.startx,
                    starty = state.backgroundNode.starty,
                    endx = state.backgroundNode.endx,
                    endy = state.backgroundNode.endy,
                    x = state.backgroundNode.x,
                    y = state.backgroundNode.y
                )
                notifyListeners(state)
                state
            }

            is ExecutionState.Finished -> {
                notifyListeners(GameState.Finished)
                GameState.Finished
            }
        }
    }

    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ExitGameLoop -> return
            is UiEvent.AdvanceExecution -> return
            is UiEvent.SelectChoice -> selectChoice(event.index)
        }
    }

    private fun selectChoice(choiceIndex: Int) {
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

    fun shutdown() {
        engineScope.cancel()
        uiEventChannel?.close()
    }
}