package dev.dokky.zerojson.internal

import io.kodec.text.AbstractSubString
import io.kodec.text.SimpleSubString
import io.kodec.text.StringTextReader
import io.kodec.text.TextReaderSubString

internal fun JsonReaderImpl.readSubString(
    destination1: TextReaderSubString,
    destination2: SimpleSubString,
    requireQuotes: Boolean,
    allowNull: Boolean,
    maxLength: Int
): AbstractSubString {
    val stringStart = position
    val scanResult = scanString(
        requireQuotes = requireQuotes,
        allowNull = allowNull,
        maxLength = maxLength,
        onMaxLength = fail,
        allowEscapes = false
    )

    if (scanResult.isEscaped) {
        position = stringStart
        readSubStringSlow(destination2, allowNull = allowNull, maxLength = maxLength)
        return destination2
    }

    val quoted = scanResult.quoted
    destination1.setUnchecked(
        input,
        start = stringStart + quoted,
        end = position - quoted,
        codePoints = scanResult.codePoints,
        hashCode = scanResult.hash
    )
    return destination1
}

internal fun JsonReaderImpl.readSubString(
    reader: StringTextReader,
    destination: SimpleSubString,
    requireQuotes: Boolean,
    allowNull: Boolean,
    maxLength: Int
) {
    val stringStart = position

    val quotes = when {
        requireQuotes -> { expectOpenQuotes(); true }
        else -> input.trySkip('"')
    }

    val scanResult = when {
        quotes -> {
            reader.scanQuotedStringContent(maxLength = maxLength, onMaxLength = fail, allowEscapes = false)
                .also { input.trySkip('"') }
                .markQuoted()
        }
        else -> scanKeyword(
            maxLength = maxLength,
            onMaxLength = fail,
            allowEscapes = false,
            allowNull = allowNull,
            allowBoolean = true
        )
    }

    if (scanResult.isEscaped) {
        position = stringStart
        readSubStringSlow(destination, allowNull = allowNull, maxLength = maxLength)
        return
    }

    val quoted = scanResult.quoted
    destination.setUnchecked(
        reader.input,
        start = stringStart + quoted,
        end = position - quoted,
        hashCode = scanResult.hash
    )
}

private fun JsonReaderImpl.readSubStringSlow(
    destination: SimpleSubString,
    maxLength: Int,
    allowNull: Boolean
) {
    config.stringBuilder.setLength(0)
    val hashCode = input.readJsonString(
        config.stringBuilder.builder,
        requireQuotes = config.expectStringQuotes,
        maxLength = maxLength,
        allowNull = allowNull
    )
    config.stringBuilder.updateCapacity()
    destination.setUnchecked(
        source = config.stringBuilder.builder,
        start = 0,
        end = config.stringBuilder.length,
        hashCode = hashCode
    )
}