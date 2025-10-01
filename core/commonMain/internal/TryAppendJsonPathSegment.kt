package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader
import io.kodec.text.readCharsInline

internal fun StringBuilder.tryAppendSegment(input: RandomAccessTextReader, keyStart: Int): Boolean {
    val oldLength = length
    val segmentStart = oldLength + 1
    append('.')

    input.position = keyStart

    var empty = false
    val malformedKey = runCatching {
        input.readJsonString(
            output = this,
            requireQuotes = false,
            allowNull = true,
            allowBoolean = true
        )
        val keyEnd = input.position

        if (this.length == segmentStart) {
            empty = true
            return@runCatching
        }

        if (stringNeedBracketedSelection(start = segmentStart, end = length)) {
            // slow path
            setLength(oldLength) // remove dot
            append('[')
            input.position = keyStart
            val needQuotes = !input.nextIs('"')
            if (needQuotes) append('"')
            input.readRange(this, keyEnd)
            if (needQuotes) append('"')
            append(']')
        }
    }.isFailure

    val invalidSegment = malformedKey || empty

    if (invalidSegment) {
        input.position = keyStart
        setLength(oldLength)
        return true
    }
    return false
}

// the only required here is dot `.`, all others included for resulting path readability
private val SPECIAL_JSON_PATH_CHARS = charArrayOf('.', '\'', '@', '*')

private fun StringBuilder.stringNeedBracketedSelection(start: Int, end: Int): Boolean {
    val firstChar = this[start]
    if (firstChar.isDigit() || firstChar == '-' || firstChar == '?') return true

    for (i in start ..< end) {
        val c = this[i]

        if (tryEscape(c) != null ||
            JsonCharClasses.isStringTerminator(c.code) ||
            c in SPECIAL_JSON_PATH_CHARS)
        {
            return true
        }
    }

    return false
}

private fun RandomAccessTextReader.readRange(output: StringBuilder, end: Int) {
    readCharsInline { c ->
        if (position >= end) return
        output.append(c)
        true
    }
}