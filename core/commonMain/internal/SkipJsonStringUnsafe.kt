package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader

/** WARN: does not check validity of the escape sequences */
internal fun JsonReaderImpl.skipStringUnsafe(
    expectQuotes: Boolean = config.expectStringQuotes
) {
    val quotes = when {
        expectQuotes -> { input.expect('"'); true }
        else -> input.trySkip('"')
    }
    when {
        quotes -> skipQuotedString()
        else -> skipUnquotedString()
    }
    skipWhitespace()
}

private fun JsonReaderImpl.skipUnquotedString() {
    val begin = position
    input.skipUnquotedStringContentUnsafe()
    if (begin == position) {
        input.unexpectedNextChar(config.messageBuilder, expected = '"'.code)
    }
}

private fun JsonReaderImpl.skipQuotedString() {
    input.skipJsonStringTemplate(
        onEof = { input.unexpectedEof() },
        terminator = { it == '"'.code }
    )
    input.readCodePoint() // skip quotes
}

internal fun JsonReaderImpl.skipUnquotedLiteralUnsafe() {
    val begin = position
    input.skipUnquotedStringContentUnsafe()
    if (begin == position) missingUnquotedLiteralError()
}

private fun JsonReaderImpl.missingUnquotedLiteralError(): Nothing = when {
    JsonCharClasses.isWhitespace(nextCodePoint) -> input.throwExpectedJsonElement()
    else -> unexpectedChar(nextCodePoint)
}

private fun RandomAccessTextReader.skipUnquotedStringContentUnsafe() {
    skipJsonStringTemplate(
        onEof = { },
        terminator = { JsonCharClasses.isToken(it) }
    )
}

// WARN: does not check validity of the escape sequences
private inline fun RandomAccessTextReader.skipJsonStringTemplate(
    onEof: () -> Unit,
    terminator: (codepoint: Int) -> Boolean
) {
    while (true) {
        if (terminator(nextCodePoint)) return
        val c = readCodePoint()
        when {
            c < 0 -> { onEof(); return }
            c == '\\'.code -> readCodePoint()
        }
    }
}