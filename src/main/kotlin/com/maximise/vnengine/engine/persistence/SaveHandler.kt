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

    fun loadSave(name: String): Pair<List<Pair<String, Int>>, MutableMap<String, Value>> {
        if (!File(SAVE_DIR + name).exists()) {
            throw RuntimeException("Save $name not found.")
        }

        val input = FileInputStream(SAVE_DIR + name)
        val variables: MutableMap<String, Value> = mutableMapOf()
        val stack: MutableList<Pair<String, Int>> = mutableListOf()

        try {
            DataInputStream(input).use { inp ->
                val variablesSize = inp.readInt()

                repeat(variablesSize) {
                    val nameLength = inp.readInt()
                    val nameBytes = ByteArray(nameLength)
                    inp.readFully(nameBytes)
                    val name = String(nameBytes, Charsets.UTF_8)

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
                        else -> throw RuntimeException("Save file $name is corrupted.")
                    }
                    variables[name] = value
                }

                val stackSize = inp.readInt()
                repeat(stackSize) {
                    val hashLength = inp.readInt()
                    val hashBytes = ByteArray(hashLength)
                    inp.readFully(hashBytes)
                    val hash = String(hashBytes, Charsets.UTF_8)

                    val index = inp.readInt()

                    stack.add(Pair(hash, index))
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Corrupted file: $name")
        }

        return Pair(stack, variables)
    }

    private fun getSaveName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
        val current = LocalDateTime.now().format(formatter)
        return current
    }

    fun makeSave(
        name: String?,
        stack: ArrayDeque<ExecutionFrame>,
        variables: MutableMap<String, Value>
    ) {
        val dir = File(SAVE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val saveName = (name ?: getSaveName()) + ".save"
        File(SAVE_DIR + saveName).createNewFile()

        val output = FileOutputStream(SAVE_DIR + saveName)

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

            out.writeInt(stack.size)
            stack.forEach { frame ->
                val bytes = frame.blockHash.toByteArray(Charsets.UTF_8)
                out.writeInt(bytes.size)
                out.write(bytes)

                out.writeInt(frame.currentIndex)
            }
        }
    }
}