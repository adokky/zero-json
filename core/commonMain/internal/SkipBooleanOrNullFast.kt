package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader

// pile of bitwise ops and only 2-3 branches on fast path
internal fun RandomAccessTextReader.trySkipBooleanOrNullFast(): Boolean {
    val pos = position

    val first4Bytes = long(
        readAsciiCode(),
        readAsciiCode(),
        readAsciiCode(),
        readAsciiCode()
    )

    run {
        when (first4Bytes) {
            false4Bytes -> if (readCodePoint() != 'e'.code) return@run
            else -> if (first4Bytes != trueBytes && first4Bytes != nullBytes) return@run
        }
        if (nextIsStringTerminator()) return true
    }

    position = pos
    return false
}

// 32 left bits will be non-zero if any byte is out of ASCII range
private fun long(b4: Int, b3: Int, b2: Int, b1: Int): Long {
    val rest = (b3 or b2 or b1) shr 7

    return rest.toLong() shl 32 or
        b4.toLong() shl 24 or
        b3.toLong() shl 16 or
        b2.toLong() shl 8 or
        b1.toLong()
}

private val false4Bytes: Long = long('f'.code, 'a'.code, 'l'.code, 's'.code)
private val trueBytes: Long  = long('t'.code, 'r'.code, 'u'.code, 'e'.code)
private val nullBytes: Long  = long('n'.code, 'u'.code, 'l'.code, 'l'.code)