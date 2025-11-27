@file:Suppress("OPT_IN_USAGE")

package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import dev.dokky.zerojson.framework.jsonEscape
import dev.dokky.zerojson.framework.jsonObject
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonDecodingErrors: AbstractDecoderTest() {
    @Test
    fun single_quotes() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<String>("\"")
        }
        assertEquals("$", ex.path)
    }

    @Test
    fun unpaired_brackets() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<List<String>>("[}")
        }
        assertEquals("$[0]", ex.path)
    }

    @Test
    fun empty_string() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<List<String>>("[s,]")
        }
        assertContains(ex.message, "trailing comma")
        assertEquals("$", ex.path)
    }

    @Test
    fun nested_list() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<List<List<String>>>("[[],[s,],[]]")
        }
        assertContains(ex.message, "trailing comma")
        assertEquals("$[1]", ex.path)
    }

    @Test
    fun nested_map() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<Map<Int, Map<String, String>>>("""{"11":{},"22":{"k":"v",},"33":{}}""")
        }
        assertContains(ex.message, "trailing comma")
        assertEquals("$['22']", ex.path)
    }

    @Test
    fun key_escaping() {
        fun ZeroJson.test(key: String, expectedPath: String, quotedOnly: Boolean = false) {
            for (quoted in setOf(true, quotedOnly)) {
                val q = if (quoted) '"' else ""
                val input = """{"k1":123, $q${key.jsonEscape()}$q:"not a number"}"""
                val ex = assertFailsWith<ZeroJsonDecodingException> {
                    decode<Map<String, Int>>(input)
                }
                val message = "text input: $input"
                assertContains(ex.message, "expected integer", message = message)
                assertEquals(expectedPath, ex.path, message)
            }
            val ex = assertFailsWith<ZeroJsonDecodingException> {
                decodeFromJsonElement<Map<String, Int>>(jsonObject {
                    "k1" eq 123
                    key eq "not a number"
                })
            }
            val message = "tree input, key $key"
            assertContains(ex.message, "expected integer", message = message)
            assertEquals(expectedPath, ex.path, message)
        }

        zjson.test("k2",             "$.k2")
        zjson.test("",               "$['']", quotedOnly = true)
        zjson.test("'k2'",           "$['\\'k2\\'']")
        zjson.test("hello_world()?", "$['hello_world()?']")
        zjson.test("hello-world!",   "$.hello-world!")
        zjson.test("123",            "$['123']")
        zjson.test("-123",           "$['-123']")
        zjson.test("dot.key",        "$['dot.key']")
        zjson.test("\nkey",          "$['\\nkey']")
        zjson.test("Привет, Мир!",   "$['Привет, Мир!']", quotedOnly = true)
    }

    @Test
    fun malformed_key() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<SimpleDataClass>("""{ "u }""")
        }
        assertEquals("$", ex.path)
    }

    @Test
    fun missing_key_quote_1() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<SimpleDataClass>("""{ unknownKey": "value" }""")
        }
        assertEquals("$", ex.path, ex.stackTraceToString())
    }

    @Test
    fun missing_key_quote_2() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<SimpleDataClass>("""{ "unknownKey: "value" }""")
        }
        assertEquals("$", ex.path, ex.stackTraceToString())
    }

    @Test
    fun unknown_key() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            zjson.decode<SimpleDataClass>("""{ "unknownKey": "value" }""")
        }
        assertEquals("$.unknownKey", ex.path, ex.stackTraceToString())
    }

    @Test
    fun empty_key() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<SimpleDataClass>("""{""""")
        }
        assertEquals("$", ex.path)
    }

    @Serializable
    private data class SimpleClass(val string: String, val int: Int)

    @Test
    fun missing_comma() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<SimpleClass>("""{ "string": "value" "int": 232 }""")
        }
        assertEquals("$", ex.path)
    }

    @Test
    fun missing_closing_bracket() {
        val ex = assertFailsWith<ZeroJsonDecodingException> {
            // language=text
            zjson.decode<SimpleClass>("""{ "string": "value", "int": 232, """)
        }
        assertEquals("$", ex.path)
    }

    @Serializable
    private class InlineRoot(val string: String, @JsonInline val inlined: Nested1) {
        @Serializable class Nested1(val int: Int, val bool: Boolean, @JsonInline val inlined: Nested2)
        @Serializable class Nested2(val long: Long, val enum: TestEnum, val simple: SimpleDataClass)
    }

    @Test
    fun path_within_inlined() {
        val json = ZeroJson { isLenient = false }

        fun test(
            expectedPath: String,
            enum: String = "\"entry1\"",
            long: String = "345678",
            string: String = "\"Just a string\"",
            int: String = "123",
            key: String = "\"_\""
        ) {
            val ex = assertFailsWith<ZeroJsonDecodingException> {
                json.decode<InlineRoot>("""
                    {
                        "enum": $enum,
                        "string": $string,
                        "long": $long,
                        "bool": true,
                        "int": $int,
                        "simple": { "key": $key }
                    }
                """.trimIndent())
            }
            assertEquals(expectedPath, ex.path, ex.stackTraceToString())
        }

        test(expectedPath = "$.enum", enum = "111")
        test(expectedPath = "$.string", string = "111")
        test(expectedPath = "$.long", long = "gfg")
        test(expectedPath = "$.int", int = "false")
        test(expectedPath = "$.simple.key", key = "%")
    }
}