package com.maximise.vnengine.engine.ui

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

class TemplateEngine {
    lateinit var result: StringBuilder
    lateinit var template: String
    var current: Int = 0
    var templateSize: Int = 0
    lateinit var passedData: Map<String, Any>
    var currentExpressionIndex: Int = 0
    var currentExpression: List<TemplateToken> = listOf()
    val currentLoopIndexes: MutableMap<String, Int> = mutableMapOf()

    fun render(templatePath: String, data: Map<String, Any>): String {
        template = File(templatePath).readText(Charsets.UTF_8)
        if (template.length > 500_000) {
            logger.warn { "Unreasonably chonky html found: $templatePath" }
        }

        result = StringBuilder()
        current = 0
        templateSize = template.length
        passedData = data

        processTemplate()

        return result.toString()
    }

    private fun peek(offset: Int = 0): Char? {
        return template.getOrNull(current + offset)
    }

    private fun advance(
        addToResult: Boolean
    ): Char {
        val res = template[current]
        current++

        if (addToResult) {
            result.append(res)
        }

        return res
    }

    private fun processTemplate() {
        while (current < templateSize) {
            if (!(peek() == '{' && peek(1) == '{')) {
                advance(true)
                continue
            }

            val command = readCommand()
            processCommand(command)
        }
    }

    private fun readCommand(): String {
        val command = StringBuilder()

        advance(false) // skipped {{
        advance(false)

        while (peek(-1) != '}' && peek(0) != '}') {
            if (peek(1) == null) {
                logger.warn { "Command not enclosed" } // TODO: error propagation
                break
            }
            command.append(advance(false))
        }

        advance(false) // skipped }}
        advance(false)

        return command.toString()
    }

    private fun processCommand(command: String) {
        when {
            command.startsWith("#if ") -> {
                val condition = command.substring(4).trim()
                processIf(condition)
            }
            command.startsWith("/if") -> {
                logger.warn { "Unexpected {{/if}} without matching {{#if}}" }
            }
            command.startsWith("#each") -> {
                val listName = command.substring(6).trim()
                processLoop(listName)
            }
            command.startsWith("/each") -> {
                logger.warn { "Unexpected {{/each}} without matching {{#each}}" }
            }
            else -> {
                val value = resolveValue(command.trim(), currentLoopIndexes)
                result.append(escapeHtml(value?.toString() ?: ""))
            }
        }
    }

    private fun processLoop(listName: String) {
        val list = resolveValue(listName, currentLoopIndexes) as List<Any>
        val loopBody = extractLoopBody()

        list.forEachIndexed { index, _ ->
            currentLoopIndexes[listName] = index

            val savedTemplate = template
            val savedCurrent = current
            val savedSize = templateSize

            template = loopBody
            current = 0
            templateSize = loopBody.length

            processTemplate()

            template = savedTemplate
            current = savedCurrent
            templateSize = savedSize
        }

        currentLoopIndexes.remove(listName)
    }

    private fun extractLoopBody(): String {
        val bodyBuilder = StringBuilder()
        var depth = 1

        while (depth > 0 && current < templateSize) {
            if (peek() != '{' || peek(1) != '{') {
                bodyBuilder.append(advance(false))
                continue
            }

            val commandStart = current
            val command = readCommand()

            when {
                command.startsWith("#each ") -> {
                    depth++
                    bodyBuilder.append(template.substring(commandStart, current))
                }
                command.startsWith("/each") -> {
                    depth--
                    if (depth > 0) {
                        bodyBuilder.append(template.substring(commandStart, current))
                    }
                }
                else -> {
                    bodyBuilder.append(template.substring(commandStart, current))
                }
            }
        }

        return bodyBuilder.toString()
    }

    private fun processIf(condition: String) {
        logger.debug { "processing if with condition $condition" }
        val tokens = tokenizeExpression(condition)

        currentExpression = tokens
        currentExpressionIndex = 0
        val value = evaluateExpression()

        val shouldInclude = when (value) {
            is Boolean -> value
            else -> true
        }

        if (!shouldInclude) {
            skipToEndIf()
        } else {
            processUntilEndIf()
        }
    }

    private fun skipToEndIf() {
        var depth = 1

        while (depth > 0 && current < templateSize) {
            if (peek() != '{' || peek(1) != '{') {
                advance(false)
                continue
            }

            val command = readCommand()

            when {
                command.startsWith("#if ") -> depth++
                command.startsWith("/if") -> depth--
            }
        }
    }

    private fun processUntilEndIf() {
        while (current < templateSize) {
            if (peek() != '{' || peek(1) != '{') {
                advance(true)
                continue
            }

            val commandStart = current
            val command = readCommand()

            when {
                command.startsWith("/if") -> {
                    return
                }
                command.startsWith("#if ") -> {
                    current = commandStart
                    val nestedCommand = readCommand()
                    processCommand(nestedCommand)
                }
                else -> {
                    current = commandStart
                    val regularCommand = readCommand()
                    processCommand(regularCommand)
                }
            }
        }
    }

    private fun tokenizeExpression(expression: String): List<TemplateToken> {
        val tokens = mutableListOf<TemplateToken>()
        var value = ""

        expression.forEach { symbol ->
            when (symbol) {
                ' ', '\n', '\t', '\r' -> {
                    val token = stringToToken(value)
                    token?.let {
                        tokens.add(token)
                    }
                    value = ""
                }
                else -> value = value + symbol
            }
        }

        if (value.isNotEmpty()) {
            val token = stringToToken(value)
            token?.let { tokens.add(it) }
        }

        return tokens
    }

    private fun stringToToken(value: String): TemplateToken? {
        return when (value) {
            ">=" -> TemplateToken.GreaterEqualOperator
            "<=" -> TemplateToken.LessEqualOperator
            "==" -> TemplateToken.EqualOperator
            "!=" -> TemplateToken.NotEqualsOperator
            "<" -> TemplateToken.LessOperator
            ">" -> TemplateToken.GreaterOperator
            "and" -> TemplateToken.AndOperator
            "or" -> TemplateToken.OrOperator
            "&&" -> TemplateToken.AndOperator
            "||" -> TemplateToken.OrOperator
            "not" -> TemplateToken.NotOperator
            "(" -> TemplateToken.OpenParenthesis
            ")" -> TemplateToken.CloseParenthesis
            "" -> null
            else -> TemplateToken.Value(value)
        }
    }

    private fun advanceExpression(): TemplateToken? {
        val token = currentExpression.getOrNull(currentExpressionIndex)
        currentExpressionIndex += 1
        return token
    }

    private fun peekExpression(offset: Int = 0): TemplateToken? {
        return currentExpression.getOrNull(currentExpressionIndex + offset)
    }

    private fun peekOperator(): BinaryTemplateExpression? {
        val token = peekExpression()

        val operator = when (token) {
            TemplateToken.EqualOperator -> BinaryTemplateExpression.EQUAL
            TemplateToken.LessOperator -> BinaryTemplateExpression.LESS
            TemplateToken.GreaterOperator -> BinaryTemplateExpression.GREATER
            TemplateToken.LessEqualOperator -> BinaryTemplateExpression.LESS_EQUAL
            TemplateToken.GreaterEqualOperator -> BinaryTemplateExpression.GREATER_EQUAL
            TemplateToken.NotEqualsOperator -> BinaryTemplateExpression.NOT_EQUAL
            TemplateToken.OrOperator -> BinaryTemplateExpression.OR
            TemplateToken.AndOperator -> BinaryTemplateExpression.AND
            else -> null
        }

        return operator
    }

    private fun evaluateOperand(): Any? {
        val token = advanceExpression()
        return when (token) {
            null -> null
            TemplateToken.OpenParenthesis -> {
                val value = evaluateExpression()
                val closePar = advanceExpression()
                if (closePar !is TemplateToken.CloseParenthesis) {
                    logger.warn { "Unclosed parenthesis: $token" }
                }
                value
            }

            TemplateToken.NotOperator -> {
                val value = evaluateExpression(UnaryTemplateExpression.NOT.bp)
                !(value as Boolean)
            }

            is TemplateToken.Value -> {
                resolveValue(token.value, currentLoopIndexes)
            }

            else -> {
                logger.warn { "Unexpected type of token: $token" }
                null
            }
        }
    }

    private fun evaluateExpression(pbp: Int = 0): Any {
        var leftExpression = evaluateOperand()

        while (true) {
            val operator = peekOperator()
            if (operator == null) break

            if (operator.lbp < pbp) break

            advanceExpression()

            val secondOperand = evaluateExpression(operator.rbp)
            leftExpression = evaluateBinaryExpression(
                leftOperand = leftExpression ?: false,
                rightOperand = secondOperand,
                operator = operator
            )
        }

        return leftExpression ?: false
    }

    private fun evaluateBinaryExpression(
        leftOperand: Any,
        rightOperand: Any,
        operator: BinaryTemplateExpression
    ): Boolean {
        return operator.apply(leftOperand, rightOperand)
    }

    private fun resolveValue(expression: String, loopIndexes: Map<String, Int> = mapOf()): Any? {
        return when {
            expression.startsWith("'") && expression.endsWith("'") ->
                expression.substring(1, expression.length - 1)
            expression.startsWith("\"") && expression.endsWith("\"") ->
                expression.substring(1, expression.length - 1)

            expression == "true" -> true
            expression == "false" -> false

            expression.toIntOrNull() != null -> expression.toInt()
            expression.toDoubleOrNull() != null -> expression.toDouble()

            else -> {
                var value: Any? = passedData
                val names = expression.split(".")
                logger.debug { "names = $names" }

                for (name in names) {
                    value = when {
                        loopIndexes.contains(name) -> { // TODO: good luck debugging it if it breaks
                            ((value as Map<String, Any?>)[name] as List<*>).getOrNull(loopIndexes[name]!!)
                        }
                        value is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            (value as Map<String, Any?>)[name]
                        }
                        else -> null
                    }

                    if (value == null) break
                }

                logger.debug { "resolved value = $value" }
                value
            }
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

sealed class TemplateToken {
    data class Value(val value: String) : TemplateToken()
    object OpenParenthesis : TemplateToken()
    object CloseParenthesis : TemplateToken()
    object EqualOperator : TemplateToken()
    object GreaterEqualOperator : TemplateToken()
    object LessEqualOperator : TemplateToken()
    object LessOperator : TemplateToken()
    object GreaterOperator : TemplateToken()
    object NotOperator : TemplateToken()
    object NotEqualsOperator: TemplateToken()
    object AndOperator: TemplateToken()
    object OrOperator: TemplateToken()
}

enum class UnaryTemplateExpression(val bp: Int) {
    NOT(5) {
        override fun apply(value: Any): Boolean {
            return when (value) {
                is Boolean -> {
                    !value
                }
                else -> {
                    logger.warn { "Failed to apply not operator to $value. Fallen back to false." }
                    false
                }
            }
        }
    };

    abstract fun apply(value: Any): Boolean
}

enum class BinaryTemplateExpression(val lbp: Int, val rbp: Int) {
    EQUAL(1, 2) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is String && r is String -> l == r
                l is Double && r is Double -> l == r
                l is Int && r is Int -> l == r
                l is Double && r is Int -> l == r.toDouble()
                l is Int && r is Double -> l.toDouble() == r
                l is Boolean && r is Boolean -> l == r
                else -> {
                    logger.warn { "Failed to apply == to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    GREATER_EQUAL(1, 2) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is Double && r is Double -> l >= r
                l is Int && r is Int -> l >= r
                l is Double && r is Int -> l >= r.toDouble()
                l is Int && r is Double -> l.toDouble() >= r
                else -> {
                    logger.warn { "Failed to apply >= to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    LESS_EQUAL(1, 2) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is Double && r is Double -> l <= r
                l is Int && r is Int -> l <= r
                l is Double && r is Int -> l <= r.toDouble()
                l is Int && r is Double -> l.toDouble() <= r
                else -> {
                    logger.warn { "Failed to apply <= to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    NOT_EQUAL(1, 2) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is String && r is String -> l != r
                l is Double && r is Double -> l != r
                l is Int && r is Int -> l != r
                l is Double && r is Int -> l != r.toDouble()
                l is Int && r is Double -> l.toDouble() != r
                l is Boolean && r is Boolean -> l != r
                else -> {
                    logger.warn { "Failed to apply != to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    LESS(1, 2) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is Double && r is Double -> l < r
                l is Int && r is Int -> l < r
                l is Double && r is Int -> l < r.toDouble()
                l is Int && r is Double -> l.toDouble() < r
                else -> {
                    logger.warn { "Failed to apply < to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    GREATER(1, 2) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is Double && r is Double -> l > r
                l is Int && r is Int -> l > r
                l is Double && r is Int -> l > r.toDouble()
                l is Int && r is Double -> l.toDouble() > r
                else -> {
                    logger.warn { "Failed to apply > to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    AND(3, 4) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is Boolean && r is Boolean -> l && r
                else -> {
                    logger.warn { "Failed to apply \"and\" to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    },
    OR(3, 4) {
        override fun apply(
            l: Any,
            r: Any
        ): Boolean {
            return when {
                l is Boolean && r is Boolean -> l || r
                else -> {
                    logger.warn { "Failed to apply \"or\" to $l and $r. Fallen back to false." }
                    false
                }
            }
        }
    };

    abstract fun apply(l: Any, r: Any): Boolean
}