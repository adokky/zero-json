package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.StringBuilderWrapper
import dev.dokky.zerojson.internal.readJsonString
import io.kodec.DecodingErrorHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadJsonStringTest: AbstractJsonStringTest<String>() {
    override fun JsonReaderImpl.readTestString(
        requireQuotes: Boolean,
        maxLength: Int,
        onMaxLength: DecodingErrorHandler<Any>,
        allowNull: Boolean,
        allowBoolean: Boolean
    ): String = buildString {
        input.readJsonString(
            this,
            requireQuotes = requireQuotes,
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowNull = allowNull,
            allowBoolean = allowBoolean,
        )
    }

    override fun checkResult(original: String, result: String) {
        assertEquals(original, result)
    }

    private fun JsonReaderImpl.readChunks() {
        var i1 = 0
        val buf = StringBuilderWrapper()
        readStringChunked(buf, requireQuotes = false, chuckSize = 3) { chunk ->
            when (++i1) {
                1 -> assertEquals("12П", chunk)
                2 -> assertEquals("45", chunk)
                else -> fail("too many chunks")
            }
        }
        assertEquals(0, buf.length)
    }

    private fun JsonReaderImpl.readSingleChunk() {
        var chunks = 0
        val buf = StringBuilderWrapper()
        val s = buildString {
            readStringChunked(buf, requireQuotes = false) { chunk ->
                append(chunk)
                assertEquals(1, ++chunks, "expected single chunk")
            }
        }
        assertEquals("12П45", s)
    }

    @Test
    fun chucked_read_unquoted() {
        val s = "12П45\""
        test(s) {
            readChunks()
            expectNextIs('"')
        }
        test(s) {
            readSingleChunk()
            expectNextIs('"')
        }
    }

    @Test
    fun chucked_read_quoted() {
        val s = "\"12П45\""
        test(s) {
            readChunks()
            expectEof()
        }
        test(s) {
            readSingleChunk()
            expectEof()
        }
    }
}