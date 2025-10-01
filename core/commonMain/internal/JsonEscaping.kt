package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.text.StringTextWriter
import io.kodec.text.TextWriter
import karamel.utils.assert
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonElement

private fun toHexChar(i: Int) : Char {
    val d = i and 0xf
    return if (d < 10) (d + '0'.code).toChar()
    else (d - 10 + 'a'.code).toChar()
}

private val ESCAPE_STRINGS: Array<String?> = arrayOfNulls<String>(93).apply {
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

/*
 * We also escape '\u2028' and '\u2029', which JavaScript interprets as
 * newline characters. This prevents eval() from failing with a syntax
 * error. http://code.google.com/p/google-gson/issues/detail?id=341
 */
internal fun tryEscape(char: Char): String? {
    val c = char.code
    return when {
        c < ESCAPE_STRINGS.size -> ESCAPE_STRINGS[c]
        c == '\u2028'.code -> "\\u2028"
        c == '\u2029'.code -> "\\u2029"
        else -> null
    }
}

internal fun TextWriter.appendEscapedChar(char: Char, escapeDepth: Int) {
    val replacement = tryEscape(char)
    if (replacement != null) {
        if (escapeDepth != EscapingDepth.INITIAL) appendAdditionalSlashes(char.code, escapeDepth)
        append(replacement)
    } else {
        append(char)
    }
}

internal fun TextWriter.appendJsonString(value: String, start: Int, end: Int, escapeDepth: Int) {
    var lastPos = 0
    for (i in start ..< end) {
        val c = value[i].code
        val replacement = tryEscape(value[i]) ?: continue
        append(value, lastPos, i) // flush prev
        if (escapeDepth != EscapingDepth.INITIAL) appendAdditionalSlashes(c, escapeDepth)
        append(replacement)
        lastPos = i + 1
    }

    if (lastPos == 0) append(value) else append(value, lastPos, value.length)
}

@InternalSerializationApi
fun String.printJsonStringTo(destination: StringBuilder) {
    JsonTextWriter(StringTextWriter(destination)).writeString(this)
}

@InternalSerializationApi
fun JsonElement.printJsonTo(destination: StringBuilder) {
    JsonTextWriter(StringTextWriter(destination)).write(this, skipNullKeys = false)
}

@InternalSerializationApi
fun String.toJsonString(): String = StringBuilder(length + 4).also { printJsonStringTo(it) }.toString()

internal fun TextWriter.appendQuotes(escapingDepth: Int) {
    if (escapingDepth == EscapingDepth.INITIAL) {
        append('"')
    } else {
        appendQuotesEscaped(escapingDepth)
    }
}

/*
slashes = 1 shl level - 1
0 0:  "
1 1:  \"
2 3:  \\\"
3 7:  \\\\\\\"
4 15: \\\\\\\\\\\\\\\"
5 31: \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\"
*/
private fun TextWriter.appendQuotesEscaped(depth: Int) {
    repeat((1 shl depth) - 1) { append('\\') }
    append('"')
}

/*
additional slashes = 1 shl level
    '\t'
0 0: \t
1 1: \\t
2 3: \\\\t
3 7: \\\\\\\\t

special cases: \ and "
additional slashes = 1 shl (level + 1) - 2
     '\'
0 0:  \\
1 2:  \\\\
2 6:  \\\\\\\\
3 14: \\\\\\\\\\\\\\\\
4 30: \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
 */
private fun TextWriter.appendAdditionalSlashes(c: Int, depth: Int) {
    var slashes = 1 shl depth
    if (c == '"'.code || c == '\\'.code) slashes = (slashes shl 1) - 1
    repeat(slashes - 1) { append('\\') }
}

internal object EscapingDepth {
    const val INITIAL = 0
    const val LIMIT = 5

    fun next(current: Int): Int = (current + 1).also {
        if (it > LIMIT) throw ZeroJsonDecodingException("too big data nested inside JSON string")
    }

    fun prev(current: Int): Int {
        assert { current > 0 }
        return current - 1
    }
}
