package com.maximise.vnengine.engine.ui

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

class TemplateEngine {

    fun render(templatePath: String, data: Map<String, Any>): String {
        logger.debug { "Rendering template: $templatePath with data keys: ${data.keys}" }

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
        File(path).readText(Charsets.UTF_8)

    private fun processConditionals(html: String, data: Map<String, Any>): String {
        var result = html
        var changed = true
        var iteration = 0

        // Process innermost {{#if}} blocks first
        while (changed && iteration < 20) {
            val before = result
            result = processInnermostConditional(result, data)
            changed = (result != before)
            iteration++
        }

        if (iteration >= 20) {
            logger.warn { "Conditional processing hit max iterations - possible infinite loop" }
        }

        return result
    }

    private fun processInnermostConditional(html: String, data: Map<String, Any>): String {
        var pos = 0
        while (pos < html.length) {
            val ifStart = html.indexOf("{{#if", pos)
            if (ifStart == -1) break

            val closeTagStart = html.indexOf("{{/if", ifStart)
            if (closeTagStart == -1) {
                logger.warn { "Unclosed {{#if}} tag" }
                break
            }

            val closeTagEnd = html.indexOf("}}", closeTagStart) + 2
            val ifTagEnd = html.indexOf("}}", ifStart) + 2
            val content = html.substring(ifTagEnd, closeTagStart)

            if (content.contains("{{#if")) {
                pos = ifStart + 5
                continue
            }

            // Extract condition
            val ifTag = html.substring(ifStart, ifTagEnd)
            val condition = ifTag.substringAfter("{{#if").substringBefore("}}").trim()

            // Evaluate condition (NEW LOGIC)
            val shouldKeep = evaluateCondition(condition, data)

            logger.trace { "Processing {{#if $condition}}: keep=$shouldKeep" }

            val replacement = if (shouldKeep) content else ""
            return html.substring(0, ifStart) + replacement + html.substring(closeTagEnd)
        }

        return html
    }

    private fun evaluateCondition(condition: String, data: Map<String, Any>): Boolean {
        // Check for comparison operators
        return when {
            " == " in condition -> {
                val (left, right) = condition.split(" == ").map { it.trim() }
                val leftValue = resolveValue(left, data)
                val rightValue = resolveValue(right, data)
                leftValue == rightValue
            }
            " != " in condition -> {
                val (left, right) = condition.split(" != ").map { it.trim() }
                val leftValue = resolveValue(left, data)
                val rightValue = resolveValue(right, data)
                leftValue != rightValue
            }
            else -> {
                // Simple truthiness check (existing logic)
                val value = data[condition]
                when (value) {
                    null -> false
                    false -> false
                    "" -> false
                    is List<*> -> value.isNotEmpty()
                    else -> true
                }
            }
        }
    }

    private fun resolveValue(expr: String, data: Map<String, Any>): String {
        return when {
            // String literal (quoted)
            expr.startsWith("'") && expr.endsWith("'") -> expr.substring(1, expr.length - 1)
            expr.startsWith("\"") && expr.endsWith("\"") -> expr.substring(1, expr.length - 1)
            // Variable lookup
            else -> data[expr]?.toString() ?: ""
        }
    }

    private fun processLoops(html: String, data: Map<String, Any>): String {
        val loopRegex = """\{\{#each\s+(\w+)\s*}}(.*?)\{\{/each\s*}}""".toRegex(
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
        )

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
        val varRegex = """\{\{\s*(\w+)\s*}}""".toRegex()

        return varRegex.replace(text) { match ->
            val key = match.groupValues[1]
            val value = data[key]?.toString() ?: ""
            escapeHtml(value)
        }
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