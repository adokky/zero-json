package dev.dokky.zerojson

import dev.dokky.zerojson.framework.ESCAPE_STRINGS
import dev.dokky.zerojson.internal.CharAndPos
import dev.dokky.zerojson.internal.readEscapeChar
import io.kodec.buffers.asBuffer
import io.kodec.text.TextDecodingException
import io.kodec.text.Utf8TextReader
import karamel.utils.enrichMessageOf
import kotlin.test.*

class ReadEscapeCharTest: AbstractJsonReaderTest() {
    @Test
    fun read_escape_char() {
        fun test(input: String, expected: Char) {
            val encoded = (input + "z").encodeToByteArray()
            val buffer = encoded.asBuffer()

            enrichMessageOf<Throwable>({ "buffer input: '$input'" }) {
                val (char, pos) = buffer.readEscapeChar(reader.input, 1)
                assertEquals(expected, char)
                assertEquals(encoded.size - 1, pos)
            }

            enrichMessageOf<Throwable>({ "reader input: '$input'" }) {
                val char = Utf8TextReader.startReadingFrom(buffer).run {
                    readCodePoint() // skip backslash
                    readEscapeChar()
                        .also { expect('z') }
                }
                assertEquals(expected, char)
            }
        }

        test("\\n", '\n')
        test("\\\\", '\\')
        test("\\\"", '"')

        assertFails { test("\\uFFFG", '!') } // meta test

        for ((code, string) in ESCAPE_STRINGS.withIndex()) {
            test(string ?: continue, code.toChar())
        }
    }

    @Test
    fun char_and_pos() {
        for (i in intArrayOf(Int.MIN_VALUE, -1, 0, 1, 255, Int.MAX_VALUE))
        for (c in charArrayOf(Char.MIN_VALUE, Char(1), Char(255), Char.MAX_VALUE)) {
            val pair = CharAndPos(c, i)
            assertEquals(c, pair.char)
            assertEquals(i, pair.position)
        }
    }

    private fun testFails(input: String, expectedMessage: String? = null) {
        val encoded = input.encodeToByteArray()
        val buffer = encoded.asBuffer()
        val reader = Utf8TextReader.startReadingFrom(buffer)

        assertFailsWith<TextDecodingException>(message = "buffer input: '$input'") {
            buffer.readEscapeChar(reader, 0)
        }.checkMessage(expectedMessage)

        assertFailsWith<TextDecodingException>(message = "reader input: '$input'") {
            reader.readEscapeChar()
        }.checkMessage(expectedMessage)
    }

    private fun TextDecodingException.checkMessage(expectedMessage: String?) {
        if (expectedMessage == null) return

        val actual = message
        assertNotNull(actual)
        if (expectedMessage !in actual) fail("invalid message: $actual")
    }

    @Test
    fun eof() {
        fun test(s: String) = testFails(s, "EOF")
        test("")
        test("u")
        test("u1")
        test("u12")
        test("u123")
    }

    @Test
    fun invalid() {
        fun test(s: String) = testFails(s, "escape")
        test("u123z")
        test("u12g4")
        test("u123씚")
        test("a")
        test("-")
        test("П")
    }
}