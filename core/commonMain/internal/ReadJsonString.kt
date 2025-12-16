package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import io.kodec.DecodingErrorHandler
import io.kodec.StringHashCode
import io.kodec.StringsUTF8
import io.kodec.text.CharToClassMapper
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.StringTextReader
import io.kodec.text.Utf8TextReader
import karamel.utils.BitDescriptors
import karamel.utils.Bits32

internal const val MAX_STRING_LENGTH_ERR_MESSAGE = "string is too large"

/** @return hash code */
internal fun RandomAccessTextReader.readJsonString(
    output: StringBuilder,
    requireQuotes: Boolean,
    maxLength: Int = ZeroJsonConfiguration.Default.maxStringLength,
    onMaxLength: DecodingErrorHandler<String> = fail,
    allowNull: Boolean = false,
    allowBoolean: Boolean = true
): Int {
    val quotes = if (requireQuotes) {
        expect('"')
        true
    } else {
        trySkip('"')
    }

    return when {
        quotes -> readQuotedString(output, maxLength, onMaxLength)
        else -> readUnquotedString(output, maxLength, onMaxLength, allowNull, allowBoolean)
    }
}

private fun RandomAccessTextReader.readQuotedString(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int {
    val hash = when (this) {
        is Utf8TextReader -> readQuotedUtf8JsonStringContent(output, maxLength = maxLength, onMaxLength = onMaxLength)
        is StringTextReader -> readQuotedJsonStringContentFast(output, maxLength = maxLength, onMaxLength = onMaxLength)
    }
    // We might have stopped due to maxLength condition right before the closing quotes.
    // The next double quote MUST NOT be part of the string content
    // because escape sequences are handled BEFORE checking maxLength.
    trySkip('"')
    return hash
}

private fun RandomAccessTextReader.readUnquotedString(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowNull: Boolean,
    allowBoolean: Boolean
): Int {
    val start = output.length
    val hash = when (this) {
        is Utf8TextReader -> readUnquotedUtf8JsonString(output, maxLength = maxLength, onMaxLength = onMaxLength)
        is StringTextReader -> readUnquotedJsonStringSlow(output, maxLength = maxLength, onMaxLength = onMaxLength)
    }
    checkUnquotedString(this, output, start = start, hash = hash, allowNull = allowNull, allowBoolean = allowBoolean)
    return hash
}

private fun Utf8TextReader.readQuotedUtf8JsonStringContent(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int = readUtf8JsonStringContentFast(
    output,
    allowEof = false,
    charClasses = JsonCharClasses.mapper,
    terminatorClass = JsonCharClasses.DOUBLE_QUOTE,
    maxLength = maxLength,
    onMaxLength = onMaxLength
)

private fun Utf8TextReader.readUnquotedUtf8JsonString(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int = readUtf8JsonStringContentFast(
    output,
    allowEof = true,
    charClasses = JsonCharClasses.mapper,
    terminatorClass = JsonCharClasses.STR_TERM,
    maxLength = maxLength,
    onMaxLength = onMaxLength
)

private fun StringTextReader.readQuotedJsonStringContentFast(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int {
    val start = position
    val closingQuotePos = input.indexOf('"', position)
    if (closingQuotePos < 0) {
        position = input.length
        unexpectedEof()
    }

    val end = minOf(closingQuotePos, start + maxLength)

    var hash = StringHashCode.init()
    for (i in start until end) {
        val c = input[i]
        hash = StringHashCode.next(hash, c)
        if (c == '\\') {
            return readQuotedJsonStringContentSlow(output, maxLength, onMaxLength)
        }
    }

    position = end
    if (end != closingQuotePos) onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE)
    output.appendRange(input, start, end)
    return hash
}

private fun StringTextReader.readQuotedJsonStringContentSlow(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int {
    val start = position
    var hash = StringHashCode.init()
    var length = 0
    var state = 0 // 0 - EOF, 1 - stopped max length, 2 - closed

    var i = start
    var nonEscStart = start
    while (i < input.length) {
        when(val c = input[i]) {
            '\\' -> {
                if (nonEscStart < i) output.appendRange(input, nonEscStart, i)
                if (++length > maxLength) { state = 1; break }
                position = i + 1
                val esc = readEscapeChar()
                hash = StringHashCode.next(hash, esc)
                output.append(esc)
                i = position
                nonEscStart = i
            }
            '"' -> { state = 2; break }
            else -> {
                if (++length > maxLength) { state = 1; break }
                hash = StringHashCode.next(hash, c)
                i++
            }
        }
    }

    when(state) {
        0 -> unexpectedEof()
        1 -> onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE)
    }

    if (nonEscStart < i) output.appendRange(input, nonEscStart, i)

    position = i
    return hash
}

private fun RandomAccessTextReader.readUnquotedJsonStringSlow(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int = scanStringContentTemplate(
    allowEof = true,
    terminator = { cp -> JsonCharClasses.mapper.hasClass(cp, JsonCharClasses.STR_TERM) },
    maxLength = maxLength,
    onMaxLength = { onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE) },
    readEscapedChar = { readEscapeChar() },
    acceptChar = { output.append(it) }
).hash

private fun <BDS : BitDescriptors> Utf8TextReader.readUtf8JsonStringContentFast(
    output: StringBuilder,
    allowEof: Boolean,
    charClasses: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int {
    val buffer = buffer
    var pos = position
    var prevPos = pos
    var hash = StringHashCode.init()
    var stringSize = 0

    StringsUTF8.readFromByteStream(
        readByte = { if (pos < buffer.size) buffer[pos++] else -1 },
        acceptChar = { c ->
            val char = when {
                c == '\\' -> {
                    val (char, newPos) = buffer.readEscapeChar(this@readUtf8JsonStringContentFast, start = pos)
                    pos = newPos
                    char
                }
                charClasses.hasClass(c.code, terminatorClass) -> {
                    position = prevPos
                    return hash
                }
                else -> c
            }
            if (++stringSize > maxLength) {
                position = prevPos
                onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE)
                return hash
            }
            hash = StringHashCode.next(hash, char)
            output.append(char)
            prevPos = pos
        }
    )

    position = pos
    if (!allowEof) unexpectedEof()
    return hash
}