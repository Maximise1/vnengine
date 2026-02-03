package com.maximise.vnengine.engine.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

//private val logger = KotlinLogging.logger {  }

class AssetLoader {
    private val screenHolder: MutableMap<String, String> = mutableMapOf()
    private val imageHolder: MutableMap<String, String> = mutableMapOf()

    companion object {
        val ALLOWED_IMAGE_TYPES = setOf( // TODO: add the rest
            "png",
            "jpg",
            "jpeg",
            "webp"
        )
    }

    init {
        loadScreens()
        loadImages()
    }

    private fun loadImages(path: String = "/home/smol/project/VNEngine/res/images") { // TODO: concurrency, duh
        val dir = File(path)
        if (dir.isFile) {
            if (ALLOWED_IMAGE_TYPES.contains(dir.extension)) {
                imageHolder.put(dir.nameWithoutExtension, dir.path)
            }
        } else {
            dir.list().forEach { name ->
                loadImages("$path/$name")
            }
        }
    }

    private fun loadScreens(path: String = "/home/smol/project/VNEngine/res/screens") { // TODO: concurrency, duh
        val dir = File(path)
        if (dir.isFile) {
            if (dir.extension == "html") {
                screenHolder.put(dir.name, dir.path)
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

    fun resolveImage(name: String): String {
        return imageHolder[name] ?: throw RuntimeException("Image $name not found")
    }
}