package dev.dokky.zerojson.internal

import io.kodec.text.DefaultCharClasses
import io.kodec.text.NumberParsingError

internal fun JsonReaderImpl.readNumberAsString(
    sb: StringBuilder,
    allowSpecialFp: Boolean = true
): String? {
    val start = position

    if (!trySkipNumber(allowSpecialFp)) {
        position = start
        return null
    }

    val length = position - start
    position = start
    sb.setLength(0)
    return input.readStringSized(length, sb)
}

internal fun JsonReaderImpl.trySkipNumber(allowSpecialFp: Boolean = true): Boolean {
    val start = position

    val parsingError = input.errorContainer.prepare<NumberParsingError>()

    input.readJsonNumber(
        readNumberResult, // ignore
        allowSpecialFp = allowSpecialFp,
        onFail = parsingError
    )

    // ignore overflows
    if (parsingError.consumeError() == NumberParsingError.MalformedNumber) {
        position = start
        return false
    }

    return true
}

internal fun JsonReaderImpl.tryReadNumberAsString(allowNaN: Boolean): String? {
    val c = nextCodePoint
    val begin = position

    // fast path cehck #1
    if (c == '-'.code || c == '.'.code) {
        input.readCodePoint()
    }

    // fast path cehck #2
    if (DefaultCharClasses.isDigit(nextCodePoint) ||
        (allowNaN && (nextCodePoint == 'I'.code || c == 'N'.code)))
    {
        position = begin
        readNumberAsString(config.stringBuilder.builder, allowSpecialFp = allowNaN)?.let {
            skipWhitespace()
            return it
        }
    }

    position = begin
    return null
}