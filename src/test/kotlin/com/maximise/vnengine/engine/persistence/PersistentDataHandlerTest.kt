package com.maximise.vnengine.engine.persistence

import com.maximise.vnengine.engine.ast.Value
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistentDataHandlerTest {

    private lateinit var handler: PersistentDataHandler
    private lateinit var testDataDir: File

    @BeforeEach
    fun setup(@TempDir tempDir: Path) {
        testDataDir = tempDir.resolve("data/persistence").toFile()
        testDataDir.mkdirs()

        handler = PersistentDataHandler()

        // Override paths to use temp directory
        setPrivateField("variablesPath", testDataDir.resolve("variables.bin").absolutePath)
        setPrivateField("dialoguePath", testDataDir.resolve("dialogue.bin").absolutePath)
    }

    private fun setPrivateField(fieldName: String, value: String) {
        val field = PersistentDataHandler::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(handler, value)
    }

    // ==================== SEEN DIALOGUE TESTS ====================

    @Test
    fun `getSeenDialogue should return empty map when file does not exist`() {
        // When
        val result = handler.getSeenDialogue()

        // Then
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `should save and load seen dialogue`() {
        // Given
        val seenDialogue = mutableMapOf(
            "main/chapter1" to 5.toShort(),
            "main/chapter2" to 12.toShort(),
            "intro" to 0.toShort()
        )

        // When
        handler.saveSeenDialogue(seenDialogue)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(3, loaded.size)
        Assertions.assertEquals(5.toShort(), loaded["main/chapter1"])
        Assertions.assertEquals(12.toShort(), loaded["main/chapter2"])
        Assertions.assertEquals(0.toShort(), loaded["intro"])
    }

    @Test
    fun `should handle empty seen dialogue map`() {
        // Given
        val emptyMap = mutableMapOf<String, Short>()

        // When
        handler.saveSeenDialogue(emptyMap)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertTrue(loaded.isEmpty())
    }

    @Test
    fun `should handle max short value in dialogue tracking`() {
        // Given
        val seenDialogue = mutableMapOf(
            "long_block" to Short.MAX_VALUE,
            "min_block" to Short.MIN_VALUE,
            "zero_block" to 0.toShort()
        )

        // When
        handler.saveSeenDialogue(seenDialogue)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(Short.MAX_VALUE, loaded["long_block"])
        Assertions.assertEquals(Short.MIN_VALUE, loaded["min_block"])
        Assertions.assertEquals(0.toShort(), loaded["zero_block"])
    }

    @Test
    fun `should handle unicode block paths in dialogue`() {
        // Given
        val seenDialogue = mutableMapOf(
            "第1章/シーン1" to 10.toShort(),
            "main/café/entrée" to 5.toShort(),
            "🎮/level1" to 3.toShort()
        )

        // When
        handler.saveSeenDialogue(seenDialogue)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(10.toShort(), loaded["第1章/シーン1"])
        Assertions.assertEquals(5.toShort(), loaded["main/café/entrée"])
        Assertions.assertEquals(3.toShort(), loaded["🎮/level1"])
    }

    @Test
    fun `should handle very long block paths`() {
        // Given
        val longPath = "a/".repeat(100) + "final"
        val seenDialogue = mutableMapOf(
            longPath to 42.toShort()
        )

        // When
        handler.saveSeenDialogue(seenDialogue)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(42.toShort(), loaded[longPath])
    }

    @Test
    fun `should overwrite existing dialogue file on save`() {
        // Given
        val firstSave = mutableMapOf("block1" to 5.toShort())
        val secondSave = mutableMapOf("block2" to 10.toShort())

        // When
        handler.saveSeenDialogue(firstSave)
        handler.saveSeenDialogue(secondSave)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(1, loaded.size)
        assertNull(loaded["block1"])
        Assertions.assertEquals(10.toShort(), loaded["block2"])
    }

    @Test
    fun `should return empty map on corrupted dialogue file`() {
        // Given - create corrupted file directly
        val dialogueFile = File(testDataDir, "dialogue.bin")
        dialogueFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        // When
        val result = handler.loadSeenDialogue()

        // Then
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle large number of dialogue entries`() {
        // Given
        val seenDialogue = mutableMapOf<String, Short>()
        repeat(1000) { i ->
            seenDialogue["block_$i"] = (i % Short.MAX_VALUE).toShort()
        }

        // When
        handler.saveSeenDialogue(seenDialogue)
        val loaded = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(1000, loaded.size)
        Assertions.assertEquals(0.toShort(), loaded["block_0"])
        Assertions.assertEquals((999 % Short.MAX_VALUE).toShort(), loaded["block_999"])
    }

    @Test
    fun `getSeenDialogue should be equivalent to loadSeenDialogue`() {
        // Given
        val seenDialogue = mutableMapOf("test" to 5.toShort())
        handler.saveSeenDialogue(seenDialogue)

        // When
        val fromGet = handler.getSeenDialogue()
        val fromLoad = handler.loadSeenDialogue()

        // Then
        Assertions.assertEquals(fromLoad, fromGet)
    }

    // ==================== VARIABLES TESTS ====================

    @Test
    fun `getVariables should return empty map when file does not exist`() {
        // When
        val result = handler.getVariables()

        // Then
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `should save and load string variables`() {
        // Given
        val variables: MutableMap<String, Value> = mutableMapOf(
            "player_name" to Value.Str("Alice"),
            "location" to Value.Str("Forest"),
            "empty" to Value.Str("")
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(3, loaded.size)
        Assertions.assertEquals(Value.Str("Alice"), loaded["player_name"])
        Assertions.assertEquals(Value.Str("Forest"), loaded["location"])
        Assertions.assertEquals(Value.Str(""), loaded["empty"])
    }

    @Test
    fun `should save and load number variables`() {
        // Given
        val variables: MutableMap<String, Value>  = mutableMapOf(
            "health" to Value.Num(100.0),
            "score" to Value.Num(9999.99),
            "zero" to Value.Num(0.0),
            "negative" to Value.Num(-50.5),
            "infinity" to Value.Num(Double.POSITIVE_INFINITY)
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(5, loaded.size)
        Assertions.assertEquals(Value.Num(100.0), loaded["health"])
        Assertions.assertEquals(Value.Num(9999.99), loaded["score"])
        Assertions.assertEquals(Value.Num(0.0), loaded["zero"])
        Assertions.assertEquals(Value.Num(-50.5), loaded["negative"])
        Assertions.assertEquals(Value.Num(Double.POSITIVE_INFINITY), loaded["infinity"])
    }

    @Test
    fun `should save and load boolean variables`() {
        // Given
        val variables: MutableMap<String, Value>  = mutableMapOf(
            "is_dead" to Value.Bool(false),
            "has_key" to Value.Bool(true),
            "flag1" to Value.Bool(false)
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(3, loaded.size)
        Assertions.assertEquals(Value.Bool(false), loaded["is_dead"])
        Assertions.assertEquals(Value.Bool(true), loaded["has_key"])
        Assertions.assertEquals(Value.Bool(false), loaded["flag1"])
    }

    @Test
    fun `should save and load mixed variable types`() {
        // Given
        val variables = mutableMapOf(
            "name" to Value.Str("Hero"),
            "level" to Value.Num(42.0),
            "alive" to Value.Bool(true),
            "weapon" to Value.Str("Sword"),
            "mana" to Value.Num(75.5),
            "flying" to Value.Bool(false)
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(6, loaded.size)
        Assertions.assertEquals(Value.Str("Hero"), loaded["name"])
        Assertions.assertEquals(Value.Num(42.0), loaded["level"])
        Assertions.assertEquals(Value.Bool(true), loaded["alive"])
        Assertions.assertEquals(Value.Str("Sword"), loaded["weapon"])
        Assertions.assertEquals(Value.Num(75.5), loaded["mana"])
        Assertions.assertEquals(Value.Bool(false), loaded["flying"])
    }

    @Test
    fun `should handle empty variables map`() {
        // Given
        val emptyMap = mutableMapOf<String, Value>()

        // When
        handler.saveVariables(emptyMap)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertTrue(loaded.isEmpty())
    }

    @Test
    fun `should handle unicode in variable names and string values`() {
        // Given
        val variables: MutableMap<String, Value>  = mutableMapOf(
            "名前" to Value.Str("太郎"),
            "emoji_test" to Value.Str("🎮🎯✨"),
            "special" to Value.Str("Quote: \"Hi\"\nNewline\tTab")
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(Value.Str("太郎"), loaded["名前"])
        Assertions.assertEquals(Value.Str("🎮🎯✨"), loaded["emoji_test"])
        Assertions.assertEquals(Value.Str("Quote: \"Hi\"\nNewline\tTab"), loaded["special"])
    }

    @Test
    fun `should handle very long variable names and values`() {
        // Given
        val longName = "x".repeat(500)
        val longValue = "Lorem ipsum ".repeat(200)
        val variables: MutableMap<String, Value>  = mutableMapOf(
            longName to Value.Str(longValue)
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(Value.Str(longValue), loaded[longName])
    }

    @Test
    fun `should overwrite existing variables file on save`() {
        // Given
        val firstSave: MutableMap<String, Value>  = mutableMapOf("var1" to Value.Str("first"))
        val secondSave: MutableMap<String, Value>  = mutableMapOf("var2" to Value.Str("second"))

        // When
        handler.saveVariables(firstSave)
        handler.saveVariables(secondSave)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(1, loaded.size)
        assertNull(loaded["var1"])
        Assertions.assertEquals(Value.Str("second"), loaded["var2"])
    }

    @Test
    fun `should return empty map on corrupted variables file`() {
        // Given - create corrupted file directly
        val varsFile = File(testDataDir, "variables.bin")
        varsFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        // When
        val result = handler.loadVariables()

        // Then
        Assertions.assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle large number of variables`() {
        // Given
        val variables = mutableMapOf<String, Value>()
        repeat(500) { i ->
            when (i % 3) {
                0 -> variables["str_$i"] = Value.Str("value_$i")
                1 -> variables["num_$i"] = Value.Num(i.toDouble())
                2 -> variables["bool_$i"] = Value.Bool(i % 2 == 0)
            }
        }

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(500, loaded.size)
        Assertions.assertEquals(Value.Str("value_0"), loaded["str_0"])
        Assertions.assertEquals(Value.Num(1.0), loaded["num_1"])
        Assertions.assertEquals(Value.Bool(true), loaded["bool_2"])
    }

    @Test
    fun `should handle special double values`() {
        // Given
        val variables: MutableMap<String, Value>  = mutableMapOf(
            "nan" to Value.Num(Double.NaN),
            "pos_inf" to Value.Num(Double.POSITIVE_INFINITY),
            "neg_inf" to Value.Num(Double.NEGATIVE_INFINITY),
            "max" to Value.Num(Double.MAX_VALUE),
            "min" to Value.Num(Double.MIN_VALUE)
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertTrue((loaded["nan"] as Value.Num).v.isNaN())
        Assertions.assertEquals(Value.Num(Double.POSITIVE_INFINITY), loaded["pos_inf"])
        Assertions.assertEquals(Value.Num(Double.NEGATIVE_INFINITY), loaded["neg_inf"])
        Assertions.assertEquals(Value.Num(Double.MAX_VALUE), loaded["max"])
        Assertions.assertEquals(Value.Num(Double.MIN_VALUE), loaded["min"])
    }

    @Test
    fun `getVariables should be equivalent to loadVariables`() {
        // Given
        val variables: MutableMap<String, Value>  = mutableMapOf("test" to Value.Str("value"))
        handler.saveVariables(variables)

        // When
        val fromGet = handler.getVariables()
        val fromLoad = handler.loadVariables()

        // Then
        Assertions.assertEquals(fromLoad, fromGet)
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    fun `should maintain data integrity across multiple save-load cycles`() {
        // Given
        val original = mutableMapOf(
            "str" to Value.Str("test"),
            "num" to Value.Num(123.45),
            "bool" to Value.Bool(true)
        )

        // When - save and load 10 times
        var current = original
        repeat(10) {
            handler.saveVariables(current)
            current = handler.loadVariables()
        }

        // Then - data should be unchanged
        Assertions.assertEquals(original, current)
    }

    @Test
    fun `should handle simultaneous dialogue and variable persistence`() {
        // Given
        val variables: MutableMap<String, Value>  = mutableMapOf("var" to Value.Str("test"))
        val dialogue = mutableMapOf("block" to 5.toShort())

        // When
        handler.saveVariables(variables)
        handler.saveSeenDialogue(dialogue)

        val loadedVars = handler.getVariables()
        val loadedDialogue = handler.getSeenDialogue()

        // Then
        Assertions.assertEquals(variables, loadedVars)
        Assertions.assertEquals(dialogue, loadedDialogue)
    }

    @Test
    fun `should handle variable names with special characters`() {
        // Given
        val variables = mutableMapOf(
            "var-with-dashes" to Value.Str("test"),
            "var_with_underscores" to Value.Num(1.0),
            "var.with.dots" to Value.Bool(true),
            "var:with:colons" to Value.Str("test2"),
            "var with spaces" to Value.Num(42.0)
        )

        // When
        handler.saveVariables(variables)
        val loaded = handler.loadVariables()

        // Then
        Assertions.assertEquals(variables, loaded)
    }
}