package com.maximise.vnengine.engine.persistence

import com.maximise.vnengine.engine.ast.SourcePos
import com.maximise.vnengine.engine.ast.Value
import com.maximise.vnengine.engine.ast.VnNode
import com.maximise.vnengine.engine.runtime.ExecutionFrame
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveHandlerTest {

    private lateinit var saveHandler: SaveHandler
    private lateinit var testSaveDir: File

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        // Use temp directory for tests
        testSaveDir = tempDir.toFile()
        saveHandler = SaveHandler()

        // Override SAVE_DIR using reflection (or make it configurable in your actual code)
        val field = SaveHandler::class.java.getDeclaredField("SAVE_DIR")
        field.isAccessible = true
        field.set(saveHandler, testSaveDir.absolutePath + "/")
    }

    @Test
    fun `should save and load empty game state`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        val variables = mutableMapOf<String, Value>()

        // When
        saveHandler.makeSave("empty_test", stack, variables)
        val (loadedStack, loadedVars) = saveHandler.loadSave("empty_test.save")

        // Then
        Assertions.assertTrue(loadedStack.isEmpty())
        Assertions.assertTrue(loadedVars.isEmpty())
    }

    @Test
    fun `should save and load all variable types`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        val variables = mutableMapOf(
            "name" to Value.Str("Alice"),
            "health" to Value.Num(100.0),
            "isDead" to Value.Bool(false),
            "score" to Value.Num(9999.99),
            "empty" to Value.Str("")
        )

        // When
        saveHandler.makeSave("vars_test", stack, variables)
        val (_, loadedVars) = saveHandler.loadSave("vars_test.save")

        // Then
        Assertions.assertEquals(5, loadedVars.size)
        Assertions.assertEquals(Value.Str("Alice"), loadedVars["name"])
        Assertions.assertEquals(Value.Num(100.0), loadedVars["health"])
        Assertions.assertEquals(Value.Bool(false), loadedVars["isDead"])
        Assertions.assertEquals(Value.Num(9999.99), loadedVars["score"])
        Assertions.assertEquals(Value.Str(""), loadedVars["empty"])
    }

    @Test
    fun `should save and load execution stack`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        stack.add(
            ExecutionFrame(
                blockHash = "main/chapter1",
                currentIndex = 5,
                block = createMockBlock("main/chapter1")
            )
        )
        stack.add(
            ExecutionFrame(
                blockHash = "main/chapter1/scene2",
                currentIndex = 12,
                block = createMockBlock("main/chapter1/scene2")
            )
        )

        val variables = mutableMapOf<String, Value>()

        // When
        saveHandler.makeSave("stack_test", stack, variables)
        val (loadedStack, _) = saveHandler.loadSave("stack_test.save")

        // Then
        Assertions.assertEquals(2, loadedStack.size)
        Assertions.assertEquals("main/chapter1", loadedStack[0].first)
        Assertions.assertEquals(5, loadedStack[0].second)
        Assertions.assertEquals("main/chapter1/scene2", loadedStack[1].first)
        Assertions.assertEquals(12, loadedStack[1].second)
    }

    @Test
    fun `should save and load complex game state`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        stack.add(
            ExecutionFrame(
                blockHash = "start",
                currentIndex = 0,
                block = createMockBlock("start")
            )
        )

        val variables = mutableMapOf(
            "player_name" to Value.Str("Hero"),
            "level" to Value.Num(5.0),
            "has_sword" to Value.Bool(true),
            "gold" to Value.Num(250.5)
        )

        // When
        val saveName = saveHandler.makeSave("complex_test", stack, variables)
        val (loadedStack, loadedVars) = saveHandler.loadSave("complex_test.save")

        // Then
        Assertions.assertEquals(1, loadedStack.size)
        Assertions.assertEquals(4, loadedVars.size)
        Assertions.assertEquals("start", loadedStack[0].first)
        Assertions.assertEquals(Value.Str("Hero"), loadedVars["player_name"])
    }

    @Test
    fun `should handle unicode characters in strings`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        val variables: MutableMap<String, Value> = mutableMapOf(
            "japanese" to Value.Str("こんにちは世界"),
            "emoji" to Value.Str("🎮🎯✨"),
            "special" to Value.Str("Quote: \"Hello\", Newline:\nTab:\t")
        )

        // When
        saveHandler.makeSave("unicode_test", stack, variables)
        val (_, loadedVars) = saveHandler.loadSave("unicode_test.save")

        // Then
        Assertions.assertEquals(Value.Str("こんにちは世界"), loadedVars["japanese"])
        Assertions.assertEquals(Value.Str("🎮🎯✨"), loadedVars["emoji"])
        Assertions.assertEquals(
            Value.Str("Quote: \"Hello\", Newline:\nTab:\t"),
            loadedVars["special"]
        )
    }

    @Test
    fun `should throw exception when loading non-existent save`() {
        // When & Then
        val exception = assertThrows<RuntimeException> {
            saveHandler.loadSave("nonexistent.save")
        }
        Assertions.assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `should handle large variable names and values`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        val longName = "x".repeat(500)
        val longValue = "Lorem ipsum ".repeat(100)
        val variables: MutableMap<String, Value> = mutableMapOf(
            longName to Value.Str(longValue)
        )

        // When
        saveHandler.makeSave("large_test", stack, variables)
        val (_, loadedVars) = saveHandler.loadSave("large_test.save")

        // Then
        Assertions.assertEquals(1, loadedVars.size)
        Assertions.assertEquals(Value.Str(longValue), loadedVars[longName])
    }

    @Test
    fun `should handle deep execution stack`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        repeat(100) { i ->
            stack.add(
                ExecutionFrame(
                    blockHash = "block_$i",
                    currentIndex = i,
                    block = createMockBlock("block_$i")
                )
            )
        }
        val variables = mutableMapOf<String, Value>()

        // When
        saveHandler.makeSave("deep_stack", stack, variables)
        val (loadedStack, _) = saveHandler.loadSave("deep_stack.save")

        // Then
        Assertions.assertEquals(100, loadedStack.size)
        Assertions.assertEquals("block_0", loadedStack[0].first)
        Assertions.assertEquals("block_99", loadedStack[99].first)
    }

    @Test
    fun `should handle negative and zero numbers`() {
        // Given
        val stack = ArrayDeque<ExecutionFrame>()
        val variables: MutableMap<String, Value> = mutableMapOf(
            "zero" to Value.Num(0.0),
            "negative" to Value.Num(-999.99),
            "small" to Value.Num(0.0001),
            "infinity" to Value.Num(Double.POSITIVE_INFINITY)
        )

        // When
        val saveName = saveHandler.makeSave("numbers_test", stack, variables)
        val (_, loadedVars) = saveHandler.loadSave("numbers_test.save")

        // Then
        Assertions.assertEquals(Value.Num(0.0), loadedVars["zero"])
        Assertions.assertEquals(Value.Num(-999.99), loadedVars["negative"])
        Assertions.assertEquals(Value.Num(0.0001), loadedVars["small"])
        Assertions.assertEquals(Value.Num(Double.POSITIVE_INFINITY), loadedVars["infinity"])
    }

    @Test
    fun `should throw exception on corrupted file`() {
        // Given - create a corrupted save file
        val corruptedFile = File(testSaveDir, "corrupted.save")
        corruptedFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5)) // Invalid data

        // When & Then
        assertThrows<RuntimeException> {
            saveHandler.loadSave("corrupted.save")
        }
    }

    @Test
    fun `should overwrite existing save with same name`() {
        // Given
        val stack1 = ArrayDeque<ExecutionFrame>()
        val vars1: MutableMap<String, Value> = mutableMapOf("test" to Value.Str("first"))
        saveHandler.makeSave("duplicate", stack1, vars1)

        val stack2 = ArrayDeque<ExecutionFrame>()
        val vars2: MutableMap<String, Value> = mutableMapOf("test" to Value.Str("second"))

        // When
        saveHandler.makeSave("duplicate", stack2, vars2)
        val (_, loadedVars) = saveHandler.loadSave("duplicate.save")

        // Then
        Assertions.assertEquals(Value.Str("second"), loadedVars["test"])
    }

    // Helper method to create mock blocks for testing
    private fun createMockBlock(id: String): VnNode.Block {
        return VnNode.Block(
            id = id,
            assignedId = null,
            name = id,
            body = listOf(),
            blocks = mapOf(),
            pos = SourcePos(0, 0)
        )
    }
}