package dev.dokky.zerojson.ktx

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongAsStringTest : JsonTestBase() {
    @Serializable
    data class HasLong(@Serializable(LongAsStringSerializer::class) val l: Long)

    @Test
    fun canSerializeAsStringAndParseBack() = parametrizedTest { 
        val original = HasLong(Long.MAX_VALUE - 1)
        val str = default.encodeToStringTest(HasLong.serializer(), original)
        assertEquals("""{"l":"9223372036854775806"}""", str)
        val restored = default.decodeFromStringTest(HasLong.serializer(), str)
        assertEquals(original, restored)
    }

    @Test
    fun canNotDeserializeInvalidString() = parametrizedTest { 
        val str = """{"l": "this is definitely not a long"}"""
        assertFailsWith<NumberFormatException> { default.decodeFromStringTest(HasLong.serializer(), str) }
        val str2 = """{"l": "1000000000000000000000"}""" // toooo long for Long
        assertFailsWith<NumberFormatException> { default.decodeFromStringTest(HasLong.serializer(), str2) }
    }
}
