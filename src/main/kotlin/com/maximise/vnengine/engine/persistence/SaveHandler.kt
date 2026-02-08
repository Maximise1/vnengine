package com.maximise.vnengine.engine.persistence

import com.maximise.vnengine.engine.ast.Value
import com.maximise.vnengine.engine.runtime.ExecutionFrame
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set

class SaveHandler {

    private val SAVE_DIR = "data/saves/"
    private val stringEncoding = Charsets.UTF_8

    fun loadSave(name: String): Save {
        if (!File("$SAVE_DIR$name.save").exists()) {
            throw RuntimeException("Save $name not found.")
        }

        val input = FileInputStream("$SAVE_DIR$name.save")

        try {
            DataInputStream(input).use { inp ->
                // name
                val nameLength = inp.readInt()
                val nameBytes = ByteArray(nameLength)
                inp.readFully(nameBytes)
                val saveName = String(nameBytes, stringEncoding)

                // id
                val id = inp.readInt()

                // image
                val imageSize = inp.readInt()
                val image = ByteArray(imageSize)
                inp.readFully(image)

                // variables
                val variables: MutableMap<String, Value> = mutableMapOf()
                val variablesSize = inp.readInt()

                repeat(variablesSize) {
                    val varNameLength = inp.readInt()
                    val varNameBytes = ByteArray(varNameLength)
                    inp.readFully(varNameBytes)
                    val varName = String(varNameBytes, stringEncoding)

                    val byte = inp.readByte()
                    val value = when (byte) {
                        0.toByte() -> {
                            val size = inp.readInt()
                            val strBytes = ByteArray(size)
                            inp.readFully(strBytes)
                            val str = String(strBytes, stringEncoding)
                            Value.Str(v = str)
                        }
                        1.toByte() -> {
                            val num = inp.readDouble()
                            Value.Num(v = num)
                        }
                        2.toByte() -> {
                            val b = inp.readBoolean()
                            Value.Bool(v = b)
                        }
                        else -> throw RuntimeException("Save file $varName.save is corrupted.")
                    }
                    variables[varName] = value
                }

                // stack
                val stackSize = inp.readInt()
                val stack: MutableList<Pair<String, Int>> = mutableListOf()
                repeat(stackSize) {
                    val hashLength = inp.readInt()
                    val hashBytes = ByteArray(hashLength)
                    inp.readFully(hashBytes)
                    val hash = String(hashBytes, stringEncoding)

                    val index = inp.readInt()

                    stack.add(Pair(hash, index))
                }

                return Save(
                    name = saveName,
                    id = id,
                    image = image,
                    stack = stack,
                    variables = variables
                )
            }
        } catch (e: IOException) {
            throw RuntimeException("Corrupted file: $name")
        }
    }

    fun loadSavePreview(filename: String): SavePreview {
        if (!File("$SAVE_DIR$filename").exists()) {
            throw RuntimeException("Save $filename not found.")
        }

        val input = FileInputStream("$SAVE_DIR$filename")

        try {
            DataInputStream(input).use { inp ->
                // name
                val nameLength = inp.readInt()
                val nameBytes = ByteArray(nameLength)
                inp.readFully(nameBytes)
                val saveName = String(nameBytes, stringEncoding)

                // id
                val id = inp.readInt()

                // image
                val imageSize = inp.readInt()
                val image = ByteArray(imageSize)
                inp.readFully(image)

                return SavePreview(
                    name = saveName,
                    id = id,
                    image = image
                )
            }
        } catch (e: IOException) {
            throw RuntimeException("Corrupted file: $filename")
        }
    }

    private fun getSaveName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
        val current = LocalDateTime.now().format(formatter)
        return current
    }

    fun listSaves(): List<String> {
        return File(SAVE_DIR).listFiles().map { file ->
            file.name
        }
    }

    fun makeSave(
        name: String?,
        lastId: Int,
        image: ByteArray,
        stack: ArrayDeque<ExecutionFrame>,
        variables: MutableMap<String, Value>
    ): Int {
        val dir = File(SAVE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val date = getSaveName()

        val saveName = (name ?: date)
        File(SAVE_DIR + date).createNewFile()

        val output = FileOutputStream("$SAVE_DIR$saveName.save")

        DataOutputStream(output).use { out ->
            // name
            val bytes = saveName.toByteArray(stringEncoding)
            out.writeInt(bytes.size)
            out.write(bytes)

            // id
            out.writeInt(lastId)

            // image
            out.writeInt(image.size)
            out.write(image)

            // variables
            out.writeInt(variables.size)
            for ((key, value) in variables) {
                val bytes = key.toByteArray(stringEncoding)
                out.writeInt(bytes.size)
                out.write(bytes)

                when (value) {
                    is Value.Str -> {
                        out.writeByte(0) // 0 for String vars
                        val bytes = value.v.toByteArray(stringEncoding)
                        out.writeInt(bytes.size)
                        out.write(bytes)
                    }
                    is Value.Num -> {
                        out.writeByte(1) // 1 for Numbers
                        out.writeDouble(value.v)
                    }
                    is Value.Bool -> {
                        out.writeByte(2) // 2 for Booleans
                        out.writeBoolean(value.v)
                    }
                }
            }

            // stack
            out.writeInt(stack.size)
            stack.forEach { frame ->
                val bytes = frame.blockHash.toByteArray(stringEncoding)
                out.writeInt(bytes.size)
                out.write(bytes)

                out.writeInt(frame.currentIndex)
            }
        }

        return lastId + 1
    }
}