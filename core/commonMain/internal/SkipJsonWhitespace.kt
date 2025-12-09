package dev.dokky.zerojson.internal

import io.kodec.text.TextReader
import karamel.utils.toInt

internal fun TextReader.skipJsonWhitespace(allowComments: Boolean) {
    while(JsonCharClasses.isWhitespace(nextCodePoint)) readCodePoint()

    if (nextCodePoint * allowComments.toInt() == '/'.code) {
        doSkipWhitespaceWithComments()
    }
}

private fun TextReader.doSkipWhitespaceWithComments() {
    while (true) {
        when {
            JsonCharClasses.isWhitespace(nextCodePoint) -> readCodePoint()
            nextCodePoint == '/'.code -> {
                readCodePoint()
                when (nextCodePoint) {
                    '/'.code -> skipLine()
                    '*'.code -> skipBlockComment()
                    else -> unexpectedChar(nextCodePoint)
                }
            }
            else -> break
        }
    }
}

private fun TextReader.unexpectedChar(code: Int): Nothing {
    if (code < 0) unexpectedEof()
    fail("unexpected character '${code.toChar()}'")
}

private fun TextReader.skipLine() {
    @Suppress("ControlFlowWithEmptyBody")
    while (readCodePoint().let { it != '\n'.code && it >= 0 }) {}
}

// starting from first character AFTER '/*'
private fun TextReader.skipBlockComment() {
    var prevIsAsterisk = false

    while (true) {
        if (readCodePoint() < 0) unexpectedEof()

        if (nextCodePoint == '*'.code) {
            prevIsAsterisk = true
        } else {
            if (prevIsAsterisk && nextCodePoint == '/'.code) {
                readCodePoint()
                return
            }
            prevIsAsterisk = false
        }
    }
}