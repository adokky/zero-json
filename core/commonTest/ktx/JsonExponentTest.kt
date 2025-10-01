package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.JsonNumberIsOutOfRange
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonDecodingException
import dev.dokky.zerojson.framework.assertFailsWith
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonExponentTest : JsonTestBase() {
    @Serializable
    private data class SomeData(val count: Long)
    @Serializable
    private data class SomeDataDouble(val count: Double)

    @Test
    fun testExponentDecodingPositive() = parametrizedTest {
        val decoded = ZeroJson.decodeFromStringTest<SomeData>("""{ "count": 23e11 }""")
        assertEquals(2300000000000, decoded.count)
    }

    @Test
    fun testExponentDecodingNegative() = parametrizedTest {
        val decoded = ZeroJson.decodeFromStringTest<SomeData>("""{ "count": -10E1 }""")
        assertEquals(-100, decoded.count)
    }

    @Test
    fun testExponentDecodingPositiveDouble() = parametrizedTest {
        val decoded = ZeroJson.decodeFromStringTest<SomeDataDouble>("""{ "count": 1.5E1 }""")
        assertEquals(15.0, decoded.count)
    }

    @Test
    fun testExponentDecodingNegativeDouble() = parametrizedTest {
        val decoded = ZeroJson.decodeFromStringTest<SomeDataDouble>("""{ "count": -1e-1 }""")
        assertEquals(-0.1, decoded.count)
    }

    @Test
    fun testExponentDecodingErrorTruncatedDecimal() = parametrizedTest {
        assertFailsWith("ZeroJsonDecodingException")
        { ZeroJson.decodeFromStringTest<SomeData>("""{ "count": -1E-1 }""") }
    }

    @Test
    fun testExponentDecodingErrorExponent() = parametrizedTest {
        assertFailsWith("ZeroJsonDecodingException")
        { ZeroJson.decodeFromStringTest<SomeData>("""{ "count": 1e-1e-1 }""") }
    }

    @Test
    fun testExponentDecodingErrorExponentDouble() = parametrizedTest {
        assertFailsWith("ZeroJsonDecodingException")
        { ZeroJson.decodeFromStringTest<SomeDataDouble>("""{ "count": 1e-1e-1 }""") }
    }

    @Test
    fun testExponentOverflowDouble() = parametrizedTest {
        assertFailsWith<ZeroJsonDecodingException> {
            ZeroJson.decodeFromStringTest<SomeDataDouble>("""{ "count": 10000e10000 }""")
        }
        assertFailsWith<ZeroJsonDecodingException> {
            ZeroJson.decodeFromStringTest<SomeDataDouble>("""{ "count": -100e2222 }""")
        }
    }

    @Test
    fun testExponentOverflow() = parametrizedTest {
        assertFailsWith<JsonNumberIsOutOfRange> {
            ZeroJson.decodeFromStringTest<SomeData>("""{ "count": 10000e10000 }""")
        }
    }

    @Test
    fun testExponentUnderflow() = parametrizedTest {
        assertFailsWith<JsonNumberIsOutOfRange> {
            ZeroJson.decodeFromStringTest<SomeData>("""{ "count": -10000e10000 }""")
        }
    }
}