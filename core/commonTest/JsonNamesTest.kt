package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNames
import kotlin.test.*

class JsonNamesTest: AbstractDecoderTest() {
    @Suppress("PropertyName", "NonAsciiCharacters", "OPT_IN_USAGE")
    @Serializable
    private data class AlternativeJsonNames(
        @JsonNames("prop2", "имя поля")
        val prop1: String,
        val ЦелочисленноеПоле: Int
    )

    @Test
    fun json_names() {
        val expected = AlternativeJsonNames("Hello world!", 3434)

        assertEquals(expected,
            zjson.decode<AlternativeJsonNames>("""{ "prop1": "${expected.prop1}", "ЦелочисленноеПоле": ${expected.ЦелочисленноеПоле} }"""))
        assertEquals(expected,
            zjson.decode<AlternativeJsonNames>("""{ "prop2": "${expected.prop1}", "ЦелочисленноеПоле": ${expected.ЦелочисленноеПоле} }"""))
        assertEquals(expected,
            zjson.decode<AlternativeJsonNames>("""{ "имя поля": "${expected.prop1}", "ЦелочисленноеПоле": ${expected.ЦелочисленноеПоле} }"""))
    }

    @Suppress("PropertyName", "NonAsciiCharacters", "OPT_IN_USAGE", "unused")
    @Serializable
    private class DuplicateJsonNames1(
        @JsonNames("prop2", "ЦелочисленноеПоле")
        val prop1: String,
        val ЦелочисленноеПоле: Int
    )

    @Suppress("OPT_IN_USAGE", "unused")
    @Serializable
    private class DuplicateJsonNames2(
        @JsonNames("prop3")
        val prop1: String,
        @JsonNames("prop3")
        val prop2: String,
    )

    @Test
    fun duplicate_json_names() {
        val json = """{"":0}"""

        assertFailsWith<SerializationException> { zjson.decode<DuplicateJsonNames1>(json) }
            .message.let {
                assertNotNull(it)
                assertTrue("ЦелочисленноеПоле" in it)
            }

        assertFailsWith<SerializationException> { zjson.decode<DuplicateJsonNames2>(json) }
            .message.let {
                assertNotNull(it)
                assertTrue("prop3" in it)
            }
    }
}