package dev.dokky.zerojson.internal

import dev.dokky.zerojson.JsonReader

@PublishedApi
internal fun JsonReaderImpl.trySkipObjectComma(): Boolean = trySkipComma('}')

@PublishedApi
internal fun JsonReaderImpl.trySkipArrayComma(): Boolean = trySkipComma(']')

/**
 * @return `false` if there are no more elements
 * (the function stops right before the closing bracket or EOF)
 */
internal fun JsonReaderImpl.trySkipComma(closingBracket: Char): Boolean {
    when(val next = nextCodePoint) {
        closingBracket.code -> return false
        ','.code -> { readCodePoint(); skipWhitespace() }
        else -> unexpectedChar(next)
    }

    if (nextCodePoint == closingBracket.code) {
        if (!config.allowTrailingComma) throwTrailingCommaIsNotAllowed()
        return false
    } else {
        if (nextCodePoint < 0) input.unexpectedEof()
        return true
    }
}

private fun JsonReader.throwTrailingCommaIsNotAllowed(): Nothing =
    fail("trailing comma is not allowed")