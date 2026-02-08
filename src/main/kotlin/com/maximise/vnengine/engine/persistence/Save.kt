package com.maximise.vnengine.engine.persistence

import com.maximise.vnengine.engine.ast.Value
import com.maximise.vnengine.engine.runtime.ExecutionFrame

data class Save(
    val name: String,
    val id: Int,
    val image: ByteArray,
    val stack: List<Pair<String, Int>>,
    val variables: MutableMap<String, Value>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Save

        if (id != other.id) return false
        if (name != other.name) return false
        if (!image.contentEquals(other.image)) return false
        if (stack != other.stack) return false
        if (variables != other.variables) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + image.contentHashCode()
        result = 31 * result + stack.hashCode()
        result = 31 * result + variables.hashCode()
        return result
    }
}