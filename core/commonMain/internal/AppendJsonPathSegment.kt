package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader

internal fun StringBuilder.tryAppendJsonPathSegment(input: RandomAccessTextReader, keyStart: Int): Boolean {
    val oldLength = this.length
    val segmentStart = oldLength + 1
    append('.')

    input.position = keyStart

    val malformedKey = runCatching {
        val onMaxLength = input.errorContainer.prepare<String>()
        input.readJsonString(
            output = this,
            requireQuotes = false,
            allowNull = true,
            allowBoolean = true,
            maxLength = 1024,
            onMaxLength = onMaxLength
        )
        onMaxLength.consumeError { append("...") }

        if (this.length == segmentStart) { // empty segment
            setLength(oldLength)
            append("['']")
            return@runCatching
        }

        if (jsonPathSegmentRequireEscaping(segmentStart = segmentStart)) { // slow path
            val escapedStart = this.length
            // append escaped sequence to the end
            appendJsonPathSegment(this, segmentStart, escapedStart)
            val escapedEnd = length

            // replace dot and old segment with prefix
            var j = oldLength
            this[j++] = '['
            this[j++] = '\''
            for (i in escapedStart ..< escapedEnd) {
                this[j++] = this[i]
            }
            setLength(j)
            append("']")
        }
    }.isFailure

    if (malformedKey) {
        input.position = keyStart
        setLength(oldLength)
        return true
    }

    return false
}

// the only required here is dot `.`, all others included for resulting path readability
private val SPECIAL_JSON_PATH_CHARS = charArrayOf('.', '\'', '@', '*', '\$', '(', ')')

internal fun CharSequence.jsonPathSegmentRequireEscaping(segmentStart: Int): Boolean {
    if (segmentStart >= length) return true // empty segment

    val firstChar = this[segmentStart]
    if (firstChar.isDigit() || firstChar == '-' || firstChar == '?') return true

    for (i in segmentStart ..< length) {
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

internal fun StringBuilder.appendJsonPathSegment(input: CharSequence, segmentStart: Int, segmentEnd: Int) {
    for (i in segmentStart ..< segmentEnd) {
        val c = input[i]
        val escaped = when (c) {
            '"' -> null
            '\'' -> "\\\'"
            else -> tryEscape(c)
        }
        if (escaped == null) append(c) else append(escaped)
    }
}