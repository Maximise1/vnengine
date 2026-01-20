package com.maximise.vnengine.engine.persistence

import com.maximise.vnengine.engine.ast.Value
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PersistentDataHandler {
    private val variablesPath: String = "data/persistence/variables.bin"
    private val dialoguePath: String = "data/persistence/dialogue.bin"

    fun getSeenDialogue(): MutableMap<String, Short> = loadSeenDialogue()
    fun getVariables(): MutableMap<String, Value> = loadVariables()

    fun loadSeenDialogue(): MutableMap<String, Short> {
        val file = File(dialoguePath)
        if (!file.exists()) {
            return mutableMapOf()
        }

        var result = mutableMapOf<String, Short>()

        try {
            val input = FileInputStream(dialoguePath)

            DataInputStream(input).use { inp ->
                val size = inp.readInt()
                val map = HashMap<String, Short>(size)

                repeat(size) {
                    val len = inp.readInt()
                    val bytes = ByteArray(len)
                    inp.readFully(bytes)
                    val key = String(bytes, Charsets.UTF_8)

                    val value = inp.readShort()
                    map[key] = value
                }
                result = map
            }

            return result
        } catch (e: IOException) {
            println("File $dialoguePath is corrupted or missing. Original error: ${e.message}")
            return result
        }
    }

    fun loadVariables(): MutableMap<String, Value> {
        val file = File(variablesPath)
        if (!file.exists()) {
            return mutableMapOf()
        }

        var result: MutableMap<String, Value> = mutableMapOf()

        try {
            val inputStream = FileInputStream(variablesPath)

            DataInputStream(inputStream).use { inp ->
                val size = inp.readInt()
                val map = HashMap<String, Value>(size)

                repeat(size) {
                    val keySize = inp.readInt()
                    val keyBytes = ByteArray(keySize)
                    inp.readFully(keyBytes)
                    val key = String(keyBytes, Charsets.UTF_8)

                    val byte = inp.readByte()
                    val value = when (byte) {
                        0.toByte() -> {
                            val size = inp.readInt()
                            val strBytes = ByteArray(size)
                            inp.readFully(strBytes)
                            val str = String(strBytes, Charsets.UTF_8)
                            Value.Str(
                                v = str
                            )
                        }
                        1.toByte() -> {
                            val num = inp.readDouble()
                            Value.Num(
                                v = num
                            )
                        }
                        2.toByte() -> {
                            val b = inp.readBoolean()
                            Value.Bool(
                                v = b
                            )
                        }
                        else -> throw RuntimeException("File $variablesPath is corrupted.")
                    }
                    map[key] = value
                }

                result = map
            }
            return result
        } catch (e: IOException) {
            println("File $variablesPath is corrupted or missing. Original error: ${e.message}")
            return result
        }
    }

    fun saveVariables(
        variables: MutableMap<String, Value>
    ) {
        val file = File(variablesPath)
        if (!file.exists()) {
            File(variablesPath.split("/").subList(0, variablesPath.split("/").size-2).joinToString("/")).mkdirs()
            file.createNewFile()
        }

        val output = FileOutputStream(variablesPath)

        DataOutputStream(output).use { out ->
            out.writeInt(variables.size)

            for ((key, value) in variables) {
                val bytes = key.toByteArray(Charsets.UTF_8)
                out.writeInt(bytes.size)
                out.write(bytes)
                when (value) {
                    is Value.Str -> {
                        out.writeByte(0) // 0 for String vars
                        val bytes = value.v.toByteArray(Charsets.UTF_8)
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
        }
    }

    fun saveSeenDialogue(
        seenDialogue: MutableMap<String, Short>
    ) {
        val file = File(dialoguePath)
        if (!file.exists()) {
            File(dialoguePath.split("/").subList(0, dialoguePath.split("/").size-2).joinToString("/")).mkdirs()
            file.createNewFile()
        }

        val output = FileOutputStream(dialoguePath)

        DataOutputStream(output).use { out ->
            out.writeInt(seenDialogue.size)

            for ((key, value) in seenDialogue) {
                val bytes = key.toByteArray(Charsets.UTF_8)
                out.writeInt(bytes.size)
                out.write(bytes)
                out.writeShort(value.toInt())
            }
        }
    }
}