package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.framework.assertFailsWithMissingField
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonOptionalTests : JsonTestBase() {

    @Suppress("EqualsOrHashCode")
    @Serializable
    internal class Data(@Required val a: Int = 0, val b: Int = 42) {

        var c = "Hello"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Data

            if (a != other.a) return false
            if (b != other.b) return false
            if (c != other.c) return false

            return true
        }
    }

    @Test
    fun testAll() = parametrizedTest { 
        assertEquals("""{"a":0,"b":42,"c":"Hello"}""",
            default.encodeToString(Data.serializer(), Data()))
        assertEquals(lenient.decodeFromStringTest(Data.serializer(), "{a:0,b:43,c:Hello}"), Data(b = 43))
        assertEquals(lenient.decodeFromStringTest(Data.serializer(), "{a:0,b:42,c:Hello}"), Data())
    }

    @Test
    fun testMissingOptionals() = parametrizedTest { 
        assertEquals(default.decodeFromStringTest(Data.serializer(), """{"a":0,"c":"Hello"}"""), Data())
        assertEquals(default.decodeFromStringTest(Data.serializer(), """{"a":0}"""), Data())
    }

    @Test
    fun testThrowMissingField() = parametrizedTest { 
        assertFailsWithMissingField {
            lenient.decodeFromStringTest(Data.serializer(), "{b:0}")
        }
    }
}
