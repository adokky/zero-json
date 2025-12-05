package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithMessage
import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.ZeroStringTextReader
import dev.dokky.zerojson.internal.readJsonPrimitive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsonTreePrimitivesTest {
    private val stringReader = ZeroStringTextReader()
    private val reader = JsonReaderImpl(
        stringReader,
        JsonReaderConfig(
            expectStringQuotes = false,
            allowSpecialFloatingPointValues = true
        )
    )

    private fun decode(input: String): JsonPrimitive {
        stringReader.startReadingFrom(input)
        return reader.readJsonPrimitive(strict = true)
    }

    private fun test(input: String, isString: Boolean = false) {
        val original = input

        fun check(input: String) {
            val decoded = decode(input)
            assertNotEquals(JsonNull, decoded, message = input)
            assertEquals(original, decoded.content, message = input)
            assertEquals(isString, decoded.isString, message = input)
        }

        if (isString) check("\"$input\"")

        check(input)
        check("$input string")

        for (c in charArrayOf(' ', '{', '}', '[', ']', ',', '"', ':', '\n', '\t')) {
            check(input + c)
        }

        if (!isString) {
            val input = input + "Z"
            val output = decode(input)
            assertTrue(output.isString, "provided non-scalar input '$input', expected a string output, but got: '${output.content}'")
        }
    }

    private fun test(isString: Boolean, vararg inputs: String) =
        inputs.forEach { test(it, isString = isString) }

    private fun test(vararg inputs: String) = test(isString = false, *inputs)

    @Test
    fun nulls() {
        assertEquals(JsonNull, decode("null"))
        assertEquals(JsonNull, decode("null "))
        assertEquals(JsonNull, decode("null:"))

        assertFailsWithMessage<SerializationException>("expected JSON element") {
            decode(" null")
        }
        assertTrue(decode("null1").isString)
        assertTrue(decode("null_").isString)
    }

    @Test
    fun integers() = test(
        "0",
        "00",
        "1",
        "-2",
        "3000",
        "-123",
        "-03451"
    )

    @Test
    fun floats() = test(
        "0.",
        "0.0",
        "0.0e1",
        "0e2",
        ".045",
        "0.005",
        "12354.0566"
    )

    @Test
    fun strings() = test(isString = true,
        "+1+",
        "h",
        "hello",
        "Hello_World",
        "Привет.Мир!",
        "True",
        "False",
        "_5656",
    )

    @Test
    fun booleans() = test("true", "false")
}