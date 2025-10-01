package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import dev.dokky.zerojson.framework.assertFailsWithMessage
import dev.dokky.zerojson.framework.jsonObject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse

class UnknownEnumEntryTest: AbstractDecoderTest() {
    private val unknownEntry = "Entry2"

    @Test
    fun first_ensure_test_entry_is_not_present() {
        assertFalse(TestEnum.entries.any { it.name == unknownEntry })
    }

    @Test
    fun streaming_decoder() {
        assertFailsWithMessage<SerializationException>("unknown entry '$unknownEntry'") {
            zjson.decode<TestEnum>("Entry2")
        }
        assertFailsWithMessage<SerializationException>("unknown entry '$unknownEntry'") {
            zjson.decode<Box<TestEnum>>("{ value: Entry2 }")
        }
    }

    @Test
    fun tree_decoder() {
        assertFailsWithMessage<SerializationException>("unknown entry '$unknownEntry'") {
            zjson.decodeFromJsonElement<TestEnum>(JsonPrimitive(unknownEntry))
        }
        assertFailsWithMessage<SerializationException>("unknown entry '$unknownEntry'") {
            zjson.decodeFromJsonElement<Box<TestEnum>>(
                jsonObject { "value" eq unknownEntry }
            )
        }
    }
}