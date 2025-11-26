/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LongAsStringTest : JsonTestBase() {
    @Serializable
    data class HasLong(@Serializable(LongAsStringSerializer::class) val l: Long)

    @Test
    fun canSerializeAsStringAndParseBack() = parametrizedTest { jsonTestingMode ->
        val original = HasLong(Long.MAX_VALUE - 1)
        val str = default.encodeToString(HasLong.serializer(), original, jsonTestingMode)
        assertEquals("""{"l":"9223372036854775806"}""", str)
        val restored = default.decodeFromString(HasLong.serializer(), str, jsonTestingMode)
        assertEquals(original, restored)
    }

    @Test
    fun canNotDeserializeInvalidString1() = parametrizedTest { jsonTestingMode ->
        val str = """{"l": "this is definitely not a long"}"""
        assertFailsWith<NumberFormatException> { default.decodeFromString(HasLong.serializer(), str, jsonTestingMode) }
    }

    @Test
    fun canNotDeserializeInvalidString2() = parametrizedTest { jsonTestingMode ->
        val str2 = """{"l": "1000000000000000000000"}""" // toooo long for Long
        assertFailsWith<NumberFormatException> { default.decodeFromString(HasLong.serializer(), str2, jsonTestingMode) }
    }
}