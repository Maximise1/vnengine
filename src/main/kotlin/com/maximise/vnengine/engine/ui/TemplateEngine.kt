package com.maximise.vnengine.engine.ui

import java.io.File

class TemplateEngine {

    fun render(templatePath: String, data: Map<String, Any>): String {
        val template = loadTemplate(templatePath)
        var html = template

        // Process conditionals FIRST
        html = processConditionals(html, data)

        // Then process loops
        html = processLoops(html, data)

        // Finally replace variables
        html = replaceVariables(html, data)

        return html
    }

    private fun loadTemplate(path: String): String =
        File(path).inputStream().readBytes().toString(Charsets.UTF_8)

    private fun processConditionals(html: String, data: Map<String, Any>): String {
        val ifRegex = """\{\{#if\s+(\w+)}}(.*?)\{\{/if}}""".toRegex(RegexOption.DOT_MATCHES_ALL)

        return ifRegex.replace(html) { matchResult ->
            val varName = matchResult.groupValues[1].trim()
            val content = matchResult.groupValues[2]

            val value = data[varName]

            // Show content if value exists and is truthy
            if (value != null && value != false && value != "") {
                content
            } else {
                ""
            }
        }
    }

    private fun processLoops(html: String, data: Map<String, Any>): String {
        val loopRegex = """\{\{#each\s+(\w+)}}(.*?)\{\{/each}}""".toRegex(RegexOption.DOT_MATCHES_ALL)

        return loopRegex.replace(html) { matchResult ->
            val arrayName = matchResult.groupValues[1].trim()
            val loopTemplate = matchResult.groupValues[2]

            @Suppress("UNCHECKED_CAST")
            val array = data[arrayName] as? List<Map<String, Any>> ?: emptyList()

            array.joinToString("") { item ->
                replaceVariables(loopTemplate, item)
            }
        }
    }

    private fun replaceVariables(text: String, data: Map<String, Any>): String {
        var result = text
        val varRegex = """\{\{(\w+)}}""".toRegex()

        varRegex.findAll(text).forEach { match ->
            val key = match.groupValues[1]
            val value = data[key]?.toString() ?: ""
            result = result.replace("{{$key}}", escapeHtml(value))
        }

        return result
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}