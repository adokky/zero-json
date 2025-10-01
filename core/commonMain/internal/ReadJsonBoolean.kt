package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader

internal fun RandomAccessTextReader.tryReadJsonBoolean(): Boolean? {
    val start = position
    val firstChar = readCodePoint()

    if (firstChar == 't'.code &&
        readCodePoint() == 'r'.code &&
        readCodePoint() == 'u'.code &&
        readCodePoint() == 'e'.code &&
        nextIsStringTerminator()) {
        return true
    } else if (firstChar  == 'f'.code &&
        readCodePoint() == 'a'.code &&
        readCodePoint() == 'l'.code &&
        readCodePoint() == 's'.code &&
        readCodePoint() == 'e'.code &&
        nextIsStringTerminator()) {
        return false
    }

    position = start
    return null
}