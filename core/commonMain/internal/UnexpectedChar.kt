package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.ReadNumberResult
import karamel.utils.buildString

internal fun RandomAccessTextReader.unexpectedNextChar(
    messageBuilder: StringBuilderWrapper,
    expected: Int
): Nothing {
    fail(messageBuilder.buildString {
        this.append("expected ")
        appendReadableJsonToken(expected)
        if (!JsonCharClasses.isWhitespace(nextCodePoint)) {
            this.append(", got ")
            appendNextJsonTokenFrom(this@unexpectedNextChar)
        }
    })
}

internal fun RandomAccessTextReader.unexpectedChar(messageBuilder: StringBuilderWrapper, codepoint: Int): Nothing {
    if (codepoint < 0) unexpectedEof()
    fail(messageBuilder.buildString {
        append("unexpected ")
        appendReadableJsonToken(codepoint)
    })
}

private const val MAX_KEYWORD_LENGTH = 20

@OptIn(ExperimentalStdlibApi::class)
private val unicodeHexFormat = HexFormat {
    number.prefix = "\\u"
    number.minLength = 4
}

@OptIn(ExperimentalStdlibApi::class)
internal fun StringBuilder.appendNextJsonTokenFrom(reader: RandomAccessTextReader) {
    // tryAppendReadableJsonToken excludes the fallowing cases:
    // - quoted string
    // - whitespace (empty unquoted string)
    if (tryAppendReadableJsonToken(reader.nextCodePoint)) return

    if (reader.trySkipJsonNull()) {
        append("null")
        return
    }

    reader.tryReadJsonBoolean()
        ?.let { append(it); return }

    reader.tryReadJsonNumber(ReadNumberResult(), allowSpecialFloatingPointValues = false)
        ?.let { append(it); return }

    append('\'')
    val outputStart = length
    val strStart = reader.position
    var malformed = false
    try {
        reader.readJsonString(
            output = this,
            requireQuotes = false,
            maxLength = MAX_KEYWORD_LENGTH + 1,
            onMaxLength = reader.errorContainer
        )
    } catch (_: ZeroJsonDecodingException) {
        malformed = true
    }

    val stringIsTooLong = reader.errorContainer.consumeError() != null
    if (stringIsTooLong) {
        append("...")
    } else {
        val strLength = length - outputStart
        if (malformed || strLength == 1 || (strLength == 0 && reader.nextCodePoint >= 0)) {
            reader.position = strStart
            val char = reader.nextCodePoint.toChar()

            if (!char.isDefined() || char.isSurrogate() || char.isISOControl()) {
                setLength(outputStart - 1) // cut off single quote
                append(tryEscape(char) ?: char.code.toHexString(unicodeHexFormat))
                return
            }

            setLength(outputStart)
            append(char)
        }
    }

    append('\'')
}

private fun StringBuilder.tryAppendReadableJsonToken(tokenCharCode: Int): Boolean {
    if (JsonCharClasses.isWhitespace(tokenCharCode)) {
        append(when (tokenCharCode) {
            ' '.code -> "whitespace (' ')"
            else -> Char(tokenCharCode).let { tryEscape(it) ?: it }
        })
        return true
    }

    append(when (tokenCharCode) {
        '{'.code -> "object"
        '['.code -> "array"
        '"'.code -> "string"
        '}'.code -> "'}'"
        ']'.code -> "']'"
        ','.code -> "','"
        ':'.code -> "':'"
        -1 -> "EOF"
        else -> return false
    })

    return true
}

private fun StringBuilder.appendReadableJsonToken(tokenCharCode: Int) {
    if (!tryAppendReadableJsonToken(tokenCharCode)) {
        append('\'')
        append(tokenCharCode.toChar())
        append('\'')
    }
}
