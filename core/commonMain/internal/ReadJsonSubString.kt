package dev.dokky.zerojson.internal

import io.kodec.DecodingErrorHandler
import io.kodec.text.*
import kotlin.jvm.JvmOverloads

/**
 * WARN: the function does NOT:
 * * check validity of the escape sequences
 * * skip any white spaces
 */
@JvmOverloads
internal fun JsonReaderImpl.readString(
    reader: Utf8TextReader,
    dest1: RandomAccessTextReaderSubString,
    dest2: SimpleSubString,
    requireQuotes: Boolean = config.expectStringQuotes,
    allowNull: Boolean = false,
    maxLength: Int = DEFAULT_MAX_STRING_LENGTH,
    onMaxLength: DecodingErrorHandler<String> = fail
): AbstractMutableSubString {
    val stringStart = position
    val scanResult = scanString(
        requireQuotes = requireQuotes,
        allowNull = allowNull,
        maxLength = maxLength,
        onMaxLength = onMaxLength,
        allowEscapes = false
    )

    if (scanResult.isEscaped) {
        position = stringStart
        readStringSlow(dest2, allowNull = allowNull, maxLength = maxLength)
        return dest2
    }

    val quoted = scanResult.quoted
    val stringEnd = position - quoted

    if (scanResult.codePoints == 0) {
        dest1.setUnchecked(reader, start = stringStart, end = stringStart, codePoints = 0)
    } else {
        dest1.setUnchecked(
            input,
            start = stringStart + quoted,
            end = stringEnd,
            codePoints = scanResult.codePoints,
            hashCode = scanResult.hash
        )
    }

    skipWhitespace()

    return dest1
}

/**
 * WARN: the function does NOT:
 * * check validity of the escape sequences
 * * skip any white spaces
 */
@JvmOverloads
internal fun JsonReaderImpl.readString(
    reader: StringTextReader,
    dest: SimpleSubString,
    requireQuotes: Boolean = config.expectStringQuotes,
    allowNull: Boolean = false,
    maxLength: Int = DEFAULT_MAX_STRING_LENGTH,
    onMaxLength: DecodingErrorHandler<String> = fail
) {
    val stringStart = position
    val scanResult = scanString(
        requireQuotes = requireQuotes,
        maxLength = maxLength,
        onMaxLength = onMaxLength,
        allowNull = allowNull,
        allowEscapes = false
    )

    if (scanResult.isEscaped) {
        position = stringStart
        readStringSlow(dest, allowNull = allowNull, maxLength = maxLength)
        return
    }

    val quoted = scanResult.quoted
    val stringEnd = position - quoted

    if (scanResult.codePoints == 0) {
        dest.setUnchecked(reader.input, start = stringStart, end = stringStart)
    } else {
        dest.setUnchecked(
            reader.input,
            start = stringStart + quoted,
            end = stringEnd,
            hashCode = scanResult.hash
        )
    }

    skipWhitespace()
}

private fun JsonReaderImpl.readStringSlow(
    dest: SimpleSubString,
    maxLength: Int,
    allowNull: Boolean
) {
    config.stringBuilder.setLength(0)
    val hashCode = input.readJsonString(
        config.stringBuilder,
        requireQuotes = config.expectStringQuotes,
        maxLength = maxLength,
        allowNull = allowNull
    )
    dest.setUnchecked(config.stringBuilder, start = 0, end = config.stringBuilder.length, hashCode = hashCode)
    skipWhitespace()
}