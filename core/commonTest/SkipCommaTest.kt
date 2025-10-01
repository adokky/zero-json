package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.ZeroStringTextReader
import dev.dokky.zerojson.internal.trySkipObjectComma
import kotlinx.serialization.SerializationException
import kotlin.test.*

class SkipCommaTest {
    private val input = ZeroStringTextReader()
    private val reader = JsonReaderImpl(input, JsonReaderConfig(allowTrailingComma = false))
    private val readerTc = JsonReaderImpl(input, JsonReaderConfig(allowTrailingComma = true))

    private fun JsonReaderImpl.assertFails(input: String, messageFragment: String? = null) {
        this@SkipCommaTest.input.startReadingFrom(input)
        val ex = assertFailsWith<SerializationException> { trySkipObjectComma() }

        if (messageFragment != null) {
            val message = ex.message
            assertNotNull(message)
            assertContains(message, messageFragment)
        }
    }

    private fun JsonReaderImpl.test(input: String, expected: Boolean = true) {
        this@SkipCommaTest.input.startReadingFrom(input)
        assertEquals(expected, trySkipObjectComma())
    }

    @Test
    fun ok() {
        for (reader in listOf(reader, readerTc)) {
            reader.test(", z}")
            input.expect('z')
            reader.test(", 123}")
            input.expect('1')
            reader.test(",\"\"}")
            input.expect('\"')
            reader.test("}", expected = false)
            input.expect('}')
        }
    }

    @Test
    fun common_failures() {
        for (reader in listOf(reader, readerTc)) {
            reader.assertFails("", "EOF")
            reader.assertFails(", ", "EOF")
            reader.assertFails(":", "expected")
            reader.assertFails("z", "expected")
            reader.assertFails(" z", "expected")
            reader.assertFails(" ]", "expected")
        }
    }

    @Test
    fun tc_failure() {
        reader.assertFails(",}", "trailing")
        reader.assertFails(",  }", "trailing")
    }

    @Test
    fun tc_ok() {
        readerTc.test(",}", expected = false)
        readerTc.test(", }", expected = false)
    }
}