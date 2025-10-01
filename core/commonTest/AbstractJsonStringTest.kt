package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.internal.JsonCharClasses
import dev.dokky.zerojson.internal.JsonReaderImpl
import io.kodec.DecodingErrorHandler
import io.kodec.text.Utf8TextReader
import kotlinx.serialization.SerializationException
import kotlin.test.*

abstract class AbstractJsonStringTest<T>: AbstractJsonReaderTest() {
    internal abstract fun JsonReaderImpl.readTestString(
        requireQuotes: Boolean = false,
        maxLength: Int = 100500,
        onMaxLength: DecodingErrorHandler<Any> = failTest,
        allowNull: Boolean = false,
        allowBoolean: Boolean = true
    ): T

    protected abstract fun checkResult(original: String, result: T)

    protected open val unquotedStringFixtures: List<String> = listOf(
        "a",
        "ABC",
        "hello",
        "_null",
        "null_",
        "Hello_World!",
        "hello/world",
        "привет",
        " 䣖㿼",
        " 䣖㿼[",
        "诼⇀",
        "\u1C85\uf330\u2222\u1111\uAAAA",
        "㶘씚&",
        "null_",
        "_null",
        "false_",
        "true_"
    )

    protected open val quotedStringFixtures: List<String> = listOf(
        "",
        " ",
        " 掲뾮",
        " 掲뾮 ",
        " ꟃ争",
        "㶘씚&",
        "null",
        "true",
        "false"
    )

    protected open val quotedStringPairs: List<Pair<String, String>> = listOf(
        "  \\t " to "  \t ",
        "\\n" to "\n",
        "\\u45F1\\n\\u00e1" to "\u45f1\n\u00e1",
        "\\u0012씚" to "\u0012씚",
        "\\\"" to "\"",
        "\\\\" to "\\",
        "\\\"\\\\ \\n" to "\"\\ \n",
        "\\\"\\\\ \\n , : . - " to "\"\\ \n , : . - "
    )

    private val stringTerminators = charArrayOf(' ', ',', ':', '[', '{', ']', '}').map { it.toString() }

    private val iterations: Int = when(GlobalTestMode) {
        TestMode.QUICK -> 10_000
        TestMode.DEFAULT -> 100_000
        TestMode.FULL -> 1_000_000
    }

    private val failTest: DecodingErrorHandler<Any> = DecodingErrorHandler {
        fail("onMaxLength() was fired")
    }

    private fun testUnquoted(string: String) {
        val s = string.filterNot {
            JsonCharClasses.mapper.hasClass(it.code, JsonCharClasses.INVALID) ||
            JsonCharClasses.isStringTerminator(it.code) ||
            it == '\\'
        }
        if (s.isEmpty()) return

        fun test(input: String, original: String = input) = test(input) {
            checkResult(original, readTestString(requireQuotes = false))
        }

        test(s)

        for (c in stringTerminators) {
            test(s + c, s)
            if (c.isEmpty()) reader.expectEof() else {
                // todo should require readTestString to skip all whitespaces
                if (c != " ") reader.expectNextIs(c.first())
            }
        }

        test("$s long,suffix", s)

        // not implemented - too costly to check every time
//        assertFailsWith<SerializationException> {
//            test(s + '\"') {
//                readTestString(requireQuotes = false)
//            }
//        }

        test("${s}_") {
            val result = readTestString(requireQuotes = false)
            assertFails { checkResult(s, result) }
        }
    }

    private val escapeSeqBreaksWithEOF = listOf(
        "\\u1",
        "\\u11",
        "\\u111",
        "\\"
    )

    private val escapeSeqBreaksWithInvalidChar = listOf(
        "\\u111\\u2222",
        "\\u111g",
        "\\uG",
        "\\uABC_",
    )

    private fun testEscapeSequenceValidation(
        data: List<String>,
        expectedMessage: String,
        quoted: Boolean
    ) {
        data.forEach { string ->
            test(if (quoted) "\"$string\"" else string) {
                assertFailsWithSerialMessage(expectedMessage) {
                    reader.readTestString()
                }
            }
        }
    }

    @Test
    fun escape_sequence_validation_quoted() {
        testEscapeSequenceValidation(escapeSeqBreaksWithEOF.filterNot { it.endsWith("\\") }, "Invalid", quoted = true)
        testEscapeSequenceValidation(escapeSeqBreaksWithEOF.filter { it.endsWith("\\") }, "EOF", quoted = true)
        testEscapeSequenceValidation(escapeSeqBreaksWithInvalidChar, "Invalid", quoted = true)
    }

    @Test
    fun escape_sequence_validation_unquoted() {
        testEscapeSequenceValidation(escapeSeqBreaksWithEOF, "EOF", quoted = false)
        testEscapeSequenceValidation(escapeSeqBreaksWithInvalidChar, "Invalid", quoted = false)
    }

    @Test
    fun quotes_requirement() {
        test("abc") {
            assertFailsWith<SerializationException> {
                reader.readTestString(requireQuotes = true)
            }
        }
    }

    @Test
    fun empty_unquoted() {
        for (term in stringTerminators + ""/*EOF*/) {
            test("xyz$term") {
                input.expect('x')
                input.expect('y')
                input.expect('z')
                assertFailsWithMessage<SerializationException>("expected string") {
                    reader.readTestString(requireQuotes = false)
                }
            }
        }
    }

    @Test
    fun max_length() {
        val input = "1234\\n56789"
        val original = "1234\n56789"
        val inputQuoted = "\"$input\""

        test(input) {
            checkResult(original, readTestString(maxLength = 10))
        }
        test(inputQuoted) {
            checkResult(original, readTestString(maxLength = 10))
        }

        test(input) {
            assertFailsWithSerialMessage("too") {
                readTestString(maxLength = 9, onMaxLength = { throw SerializationException("too long") })
            }
        }
        test(inputQuoted) {
            assertFailsWithSerialMessage("too") {
                readTestString(maxLength = 9, onMaxLength = { throw SerializationException("too long") })
            }
        }
    }

    @Test
    fun max_length_position_unquoted() {
        test("12Ё4\\n5Г789", offset = 0) {
            checkResult("12Ё4\n5Г", readTestString(maxLength = 7, onMaxLength = {}))
            assertEquals(if (input is Utf8TextReader) 10 else 8, input.position)
        }
        test("б1\\nY", offset = 0) {
            checkResult("б1\n", readTestString(maxLength = 3, onMaxLength = {}))
            assertEquals(if (input is Utf8TextReader) 5 else 4, input.position)
        }
    }

    @Test
    fun max_length_position_quoted() {
        test("\"12Ё4\\n5Г789\"", offset = 0) {
            checkResult("12Ё4\n5Г", readTestString(maxLength = 7, onMaxLength = {}))
            assertEquals(if (input is Utf8TextReader) 11 else 9, input.position)
        }
        test("\"б1\\nY\"", offset = 0) {
            checkResult("б1\n", readTestString(maxLength = 3, onMaxLength = {}))
            assertEquals(if (input is Utf8TextReader) 6 else 5, input.position)
            checkResult("Y", readTestString(maxLength = 2, onMaxLength = {}))
            assertEquals(if (input is Utf8TextReader) 7 else 6, input.position)
        }
    }

    @Test
    fun unquoted_strings_fixtures() {
        for (string in unquotedStringFixtures) {
            testUnquoted(string)
        }
    }

    @Test
    fun unquoted_random_strings() {
        for (string in StringsDataSet.getUtfData().take(iterations)) {
            testUnquoted(string)
        }
    }

    @Test
    fun unquoted_nulls() {
        for (s in listOf("null", "null,", "null ")) {
            test(s) {
                val msg = assertFailsWith<SerializationException> {
                    reader.readTestString(requireQuotes = false, allowNull = false, allowBoolean = true)
                }.message
                assertNotNull(msg)
                assertContains(msg, "string")
                assertContains(msg, "null")
            }
            test(s) {
                checkResult("null", reader.readTestString(requireQuotes = false, allowNull = true, allowBoolean = false))
            }
        }
    }

    @Test
    fun unquoted_booleans() {
        for ((input, original) in listOf(
            "false" to "false",
            "true" to "true",
            "false," to "false",
            "true," to "true"
        )) {
            test(input) {
                val msg = assertFailsWith<SerializationException> {
                    reader.readTestString(requireQuotes = false, allowNull = true, allowBoolean = false)
                }.message
                assertNotNull(msg)
                assertContains(msg, "string")
                assertContains(msg, "boolean")
            }
            test(input) {
                checkResult(original, reader.readTestString(requireQuotes = false, allowNull = false, allowBoolean = true))
            }
        }
    }

    @Test
    fun quoted_strings() {
        fun testSingle(string: String, original: String = string) {
            for (requireQuotes in booleanArrayOf(false, true)) {
                test("\"$string\"&") {
                    checkResult(original, readTestString(requireQuotes = requireQuotes))
                    reader.expectNextIs('&')
                }
                test("\"$string\"") {
                    checkResult(original, readTestString(requireQuotes = requireQuotes))
                    reader.expectEof()
                }
            }
        }

        fun test(string: String) {
            testSingle(string)
            testSingle(" $string")
            testSingle("$string ")
            testSingle("$string,")
        }

        for (string in quotedStringFixtures) {
            test(string)
        }

        for ((string, original) in quotedStringPairs) {
            testSingle(string, original)
        }

        for (string in StringsDataSet.getUtfData().take(iterations)) {
            testSingle(string.jsonEscape(), string)
        }
    }
}