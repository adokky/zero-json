@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import dev.dokky.zerojson.framework.jsonObject
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlin.test.Test

class JsonTypeMismatchTest: AbstractDecoderTest(ZeroJsonConfiguration.KotlinxJson) {
    @Test
    fun expected_int() = assertDecodingFails<Int>(JsonPrimitive("hello"), "expected integer, got 'hello'")

    @Test
    fun expected_int_got_array() {
        assertDecodingFails<Int>(buildJsonArray {}, "expected")
        assertDecodingFails<Int>(buildJsonArray { add(343) }, "expected")
    }

    @Test
    fun expected_int_got_object() {
        assertDecodingFails<Int>(jsonObject {}, "expected")
        assertDecodingFails<Int>(jsonObject { "k" eq 42 }, "expected")
    }

    @Test
    fun expected_float() {
        assertDecodingFails<Float>(JsonPrimitive("hello"), "expected number, got 'hello'")
        assertDecodingFails<Double>(JsonPrimitive("hello"), "expected number, got 'hello'")
    }

    @Test
    fun expected_string_got_int() {
        assertDecodingFails<String>(JsonPrimitive(3443), "expected")
        assertDecoded<String>("3443", JsonPrimitive(3443), ZeroJson)
    }

    @Test
    fun expected_string_got_bool() {
        assertDecodingFails<String>(JsonPrimitive(true), "expected")
        assertDecoded<String>("true", JsonPrimitive(true), ZeroJson)
    }

    @Test
    fun expected_string_got_null() {
        assertDecodingFails<String>(JsonNull)
    }

    @Test
    fun expected_string_got_array() {
        assertDecodingFails<String>(buildJsonArray {})
        assertDecodingFails<String>(buildJsonArray { add(343) })
    }

    @Test
    fun expected_string_got_object() {
        assertDecodingFails<String>(jsonObject {})
        assertDecodingFails<String>(jsonObject { "k" eq 42 })
    }
}