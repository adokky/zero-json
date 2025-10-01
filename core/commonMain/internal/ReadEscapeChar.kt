package dev.dokky.zerojson.internal

import io.kodec.StringsASCII
import io.kodec.buffers.Buffer
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.TextReader
import karamel.utils.asLong
import kotlin.jvm.JvmInline

internal fun RandomAccessTextReader.readEscapeChar(): Char = readEscapeCharInline(this, ::readCodePoint, ::readEscapeHexChar)

internal fun Buffer.readEscapeChar(reader: TextReader, start: Int): CharAndPos {
    var pos = start
    val c = readEscapeCharInline(reader,
        readCode = { if (pos < size) get(pos++) else -1 },
        readHexChar = { readEscapeHexChar(reader, pos).also { pos += 4 } }
    )
    return CharAndPos(c, pos)
}

@JvmInline
internal value class CharAndPos(val asLong: Long) {
    constructor(char: Char, position: Int): this((char.code.toLong() shl 32) or position.asLong())

    val char: Char get() = (asLong ushr 32).toInt().toChar()
    val position: Int get() = asLong.toInt()

    operator fun component1(): Char = char
    operator fun component2(): Int = position
    override fun toString(): String = "($char, $position)"
}

private fun getHexChar(reader: TextReader, code: Int): Int {
    if (code in '0'.code..'9'.code) return code - '0'.code

    val lc = code or StringsASCII.LOWER_CASE_BIT
    if (lc in 'a'.code..'f'.code) return lc - 'a'.code + 10

    reader.throwInvalidEscapeChar(code)
}

private fun TextReader.throwInvalidEscapeChar(code: Int): Nothing {
    if (code >= 0) fail("Invalid escape character") else unexpectedEof()
}

private val ESCAPE_TO_CHAR = CharArray(0x7f).also { map ->
    fun c2esc(c: Int, esc: Char) { map[esc.code] = c.toChar() }
    fun c2esc(c: Char, esc: Char) = c2esc(c.code, esc)

    c2esc(0x08, 'b')
    c2esc(0x09, 't')
    c2esc(0x0a, 'n')
    c2esc(0x0c, 'f')
    c2esc(0x0d, 'r')
    c2esc('/', '/')
    c2esc('"', '"')
    c2esc('\\', '\\')
}

private inline fun readEscapeCharInline(
    reader: TextReader,
    readCode: () -> Int,
    readHexChar: () -> Char,
): Char {
    val b = readCode()
    if (b == 'u'.code) return readHexChar()
    val mapped = ESCAPE_TO_CHAR.getOrElse(b) { 0.toChar() }
    if (mapped == 0.toChar()) reader.throwInvalidEscapeChar(b)
    return mapped
}

private inline fun readEscapeHexCharInline(
    reader: TextReader,
    c0: () -> Int,
    c1: () -> Int,
    c2: () -> Int,
    c3: () -> Int,
): Char =
      ((getHexChar(reader, c0()) shl 12)
    or (getHexChar(reader, c1()) shl 8)
    or (getHexChar(reader, c2()) shl 4)
    or  getHexChar(reader, c3())).toChar()

private fun RandomAccessTextReader.readEscapeHexChar(): Char {
    return readEscapeHexCharInline(this, ::readAsciiCode, ::readAsciiCode, ::readAsciiCode, ::readCodePoint)
}

private fun Buffer.readEscapeHexChar(reader: TextReader, start: Int): Char =
    readEscapeHexCharInline(reader,
        c0 = { getOrNegative(start) },
        c1 = { getOrNegative(start + 1) },
        c2 = { getOrNegative(start + 2) },
        c3 = { getOrNegative(start + 3) }
    )

private fun Buffer.getOrNegative(pos: Int): Int = if (pos < size) get(pos) else -1