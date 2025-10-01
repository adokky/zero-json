package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader

internal fun RandomAccessTextReader.trySkipJsonNull(): Boolean {
    val pos = position

    if (readAsciiCode() == 'n'.code &&
        readAsciiCode() == 'u'.code &&
        readAsciiCode() == 'l'.code &&
        readAsciiCode() == 'l'.code &&
        nextIsStringTerminator())
    {
        return true
    }

    position = pos
    return false
}