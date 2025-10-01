package dev.dokky.zerojson.ktx

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonRootLevelNullTest : JsonTestBase() {

    @Serializable
    private data class Simple(val a: Int = 42)

    @Test
    fun testNullableEncode() {
        // Top-level nulls in tagged encoder is not yet supported, no parametrized test
        val obj: Simple? = null
        val json = default.encodeToStringTest(Simple.serializer().nullable, obj)
        assertEquals("null", json)
    }

    @Test
    fun testNullableDecode() = parametrizedTest {
        val result = default.decodeFromStringTest(Simple.serializer().nullable, "null")
        assertNull(result)
    }
}
