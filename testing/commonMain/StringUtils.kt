package dev.dokky.zerojson.framework

import karamel.utils.containsAny
import kotlin.random.Random
import kotlin.random.nextInt

private fun toHexChar(i: Int) : Char {
    val d = i and 0xf
    return if (d < 10) (d + '0'.code).toChar()
    else (d - 10 + 'a'.code).toChar()
}

val ESCAPE_STRINGS: Array<String?> = arrayOfNulls<String>(93).apply {
    for (c in 0..0x1f) {
        val c1 = toHexChar(c shr 12)
        val c2 = toHexChar(c shr 8)
        val c3 = toHexChar(c shr 4)
        val c4 = toHexChar(c)
        this[c] = "\\u$c1$c2$c3$c4"
    }
    this['"'.code] = "\\\""
    this['\\'.code] = "\\\\"
    this['\t'.code] = "\\t"
    this['\b'.code] = "\\b"
    this['\n'.code] = "\\n"
    this['\r'.code] = "\\r"
    this[0x0c] = "\\f"
}

fun String.jsonEscape(): String {
    val sb = StringBuilder()
    val escaped = jsonEscapeTo(sb)
    return if (escaped) sb.toString() else this
}

fun String.shouldBeEscaped(): Boolean = any { ESCAPE_STRINGS.getOrNull(it.code) != null }

fun String.jsonEscapeTo(output: Appendable): Boolean {
    var escaped = false

    for (c in this@jsonEscapeTo) {
        val replacement = ESCAPE_STRINGS.getOrNull(c.code)
        if (replacement == null) {
            output.append(c)
        } else {
            escaped = true
            output.append(replacement)
        }
    }

    return escaped
}

fun generateRandomUnicodeString(size: Int): String {
    return buildString(size) {
        repeat(size) {
            val pickEscape = Random.nextBoolean()
            if (pickEscape) {
                // Definitely an escape symbol
                append(ESCAPE_STRINGS.random().takeIf { it != null } ?: 'N')
            } else {
                // Any symbol, including escaping one
                append(Char(Random.nextInt(Char.MIN_VALUE.code..Char.MAX_VALUE.code))
                    .takeIf { it.isDefined() && !it.isSurrogate()} ?: 'U')
            }
        }
    }
}

fun String.removeWhitespaces(): String = filterNot { it.isWhitespace() }

@Suppress("NOTHING_TO_INLINE") // inline to not break trimMargin optimization
inline fun String.trimMarginAndRemoveWhitespaces(): String = trimMargin().removeWhitespaces()

fun String.removeLineBreaks(): String {
    if (!containsAny('\n', '\r', '\t')) return this

    return buildString(length) {
        for (c in this@removeLineBreaks) {
            append(when (c) { '\n', '\r', '\t' -> ' '; else -> c })
        }
    }
}