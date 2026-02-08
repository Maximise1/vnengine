package com.maximise.vnengine.engine.persistence

import org.luaj.vm2.LuaValue
import java.util.Base64

data class SavePreview(
    val name: String,
    val id: Int,
    val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SavePreview

        if (id != other.id) return false
        if (name != other.name) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + image.contentHashCode()
        return result
    }

    fun toMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()

        map.put("name", name)
        map.put("id", id)
        map.put("image", byteArrayToBase64DataUrl(image))

        return map
    }

    fun byteArrayToBase64DataUrl(imageBytes: ByteArray): String {
        val base64 = Base64.getEncoder().encodeToString(imageBytes)
        return "data:image/webp;base64,$base64"
    }
}