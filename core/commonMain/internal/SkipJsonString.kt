package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import io.kodec.DecodingErrorHandler

internal fun JsonReaderImpl.skipString(
    expectQuotes: Boolean = config.expectStringQuotes,
    maxLength: Int = ZeroJsonConfiguration.Default.maxStringLength,
    onMaxLength: DecodingErrorHandler<String> = fail,
    allowNull: Boolean = false,
    allowBoolean: Boolean = true
) {
    val quotes = when {
        expectQuotes -> { expectOpenQuotes(); true }
        else -> input.trySkip('"')
    }
    when {
        quotes -> skipQuotedString(maxLength = maxLength, onMaxLength = onMaxLength)
        else -> skipUnquotedString(maxLength = maxLength, onMaxLength = onMaxLength, allowNull = allowNull, allowBoolean = allowBoolean)
    }
    skipWhitespace()
}

private fun JsonReaderImpl.skipUnquotedString(
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowNull: Boolean,
    allowBoolean: Boolean
) {
    val begin = position
    val result = input.scanJsonStringContent(
        allowEof = true,
        allowEscapes = true,
        JsonCharClasses.mapper,
        JsonCharClasses.STR_TERM,
        maxLength = maxLength,
        onMaxLength = onMaxLength
    )
    if (begin == position) {
        input.unexpectedNextChar(config.messageBuilder, expected = '"'.code)
    }
    checkUnquotedString(input,
        start = begin,
        hash = result.hash,
        allowNull = allowNull,
        allowBoolean = allowBoolean
    )
}

private fun JsonReaderImpl.skipQuotedString(maxLength: Int, onMaxLength: DecodingErrorHandler<String>) {
    input.scanJsonStringContent(
        allowEof = false,
        allowEscapes = true,
        JsonCharClasses.mapper,
        JsonCharClasses.DOUBLE_QUOTES,
        maxLength = maxLength,
        onMaxLength = onMaxLength
    )
    input.trySkip('"')
}