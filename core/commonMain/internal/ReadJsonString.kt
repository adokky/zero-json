package dev.dokky.zerojson.internal

import io.kodec.DecodingErrorHandler
import io.kodec.StringHashCode
import io.kodec.StringsUTF8
import io.kodec.text.CharToClassMapper
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.Utf8TextReader
import karamel.utils.BitDescriptors
import karamel.utils.Bits32

internal const val DEFAULT_MAX_STRING_LENGTH = 65535
internal const val MAX_STRING_LENGTH_ERR_MESSAGE = "string is too large"

/** @return hash code */
internal fun RandomAccessTextReader.readJsonString(
    output: StringBuilder,
    requireQuotes: Boolean,
    maxLength: Int = DEFAULT_MAX_STRING_LENGTH,
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

    if (quotes) {
        val hash = if (this is Utf8TextReader)
            readQuotedUtf8JsonStringContent(output, maxLength = maxLength, onMaxLength = onMaxLength) else
            readQuotedJsonStringSlowContent(output, maxLength = maxLength, onMaxLength = onMaxLength)
        trySkip('"')
        return hash
    }

    val start = output.length
    val hash = if (this is Utf8TextReader)
        readUnquotedUtf8JsonString(output, maxLength = maxLength, onMaxLength = onMaxLength) else
        readUnquotedJsonStringSlow(output, maxLength = maxLength, onMaxLength = onMaxLength)
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
    terminatorClass = JsonCharClasses.DOUBLE_QUOTES,
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

private fun RandomAccessTextReader.readQuotedJsonStringSlowContent(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int = scanJsonStringContentTemplate(
    allowEof = false,
    terminator = { it == '"'.code },
    maxLength = maxLength,
    onMaxLength = { onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE) },
    readEscapedChar = { readEscapeChar() },
    acceptChar = { output.append(it) }
).hash

private fun RandomAccessTextReader.readUnquotedJsonStringSlow(
    output: StringBuilder,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): Int = scanJsonStringContentTemplate(
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
        readByte = {
            if (pos < buffer.size) buffer[pos++] else {
                position = pos
                if (!allowEof) unexpectedEof()
                return hash
            }
        },
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

    error("this must be unreachable")
}