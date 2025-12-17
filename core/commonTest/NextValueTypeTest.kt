package dev.dokky.zerojson

import dev.dokky.zerojson.JsonReader.ValueType.*
import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals

class NextValueTypeTest: AbstractJsonReaderTest() {
    @Test
    fun testNextValueTypeString() {
        testNextValueType("\"hello\"", STRING)
        testNextValueType("\"\"", STRING)
        testNextValueType("\"\\\"\"", STRING)
    }

    @Test
    fun testNextValueTypeNumber() {
        testNextValueType("123", NUMBER)
        testNextValueType("-456", NUMBER)
        testNextValueType("0", NUMBER)
        testNextValueType("123.456", NUMBER)
        testNextValueType("-0.123", NUMBER)
        testNextValueType("1e10", NUMBER)
        testNextValueType("1E-5", NUMBER)
    }

    @Test
    fun testNextValueTypeBoolean() {
        testNextValueType("true", BOOLEAN)
        testNextValueType("false", BOOLEAN)
    }

    @Test
    fun testNextValueTypeNull() {
        testNextValueType("null", NULL)
    }

    @Test
    fun testNextValueTypeObject() {
        testNextValueType("{}", OBJECT)
        testNextValueType("{\"a\":1}", OBJECT)
        testNextValueType("{\"a\":1,\"b\":2}", OBJECT)
    }

    @Test
    fun testNextValueTypeArray() {
        testNextValueType("[]", ARRAY)
        testNextValueType("[1,2,3]", ARRAY)
        testNextValueType("[{\"a\":1}]", ARRAY)
    }

    @Test
    fun testNextValueTypeWhitespace() {
        testNextValueType(" \n\t 123", NUMBER)
        testNextValueType(" \n\t \"hello\"", STRING)
        testNextValueType(" \n\t true", BOOLEAN)
        testNextValueType(" \n\t null", NULL)
        testNextValueType(" \n\t {}", OBJECT)
        testNextValueType(" \n\t []", ARRAY)
    }

    @Test
    fun testNextValueTypeQuickGuess() {
        testNextValueType("123", NUMBER, true)
        testNextValueType("\"hello\"", STRING, true)
        testNextValueType("true", BOOLEAN, true)
        testNextValueType("null", NULL, true)
        testNextValueType("{}", OBJECT, true)
        testNextValueType("[]", ARRAY, true)
    }

    @Test
    fun testNextValueTypeInvalidInput() {
        testIncorrectValueType("invalid")
        testIncorrectValueType("123invalid")
        testIncorrectValueType("trueinvalid")
        testIncorrectValueType("nullinvalid")
    }

    private fun testNextValueType(input: String, expected: JsonReader.ValueType, quickGuess: Boolean = false) {
        val reader = JsonReader.startReadingFrom(input)
        val start = reader.position
        val actual = reader.nextValueType(quickGuess)
        assertEquals(actual, expected, "Expected $expected but got $actual for input: $input")
        assertEquals(start, reader.position)
    }

    private fun testIncorrectValueType(input: String) {
        assertFailsWithMessage<SerializationException>("unexpected") {
            JsonReader.startReadingFrom(input).nextValueType()
        }
    }
}
