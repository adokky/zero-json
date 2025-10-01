package dev.dokky.zerojson

import dev.dokky.zerojson.internal.appendNextJsonTokenFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppendNextJsonTokenTest: AbstractJsonReaderTest() {
    private fun assertNextTokenIs(name: String) {
        assertEquals(
            name,
            buildString {
                appendNextJsonTokenFrom(reader.input)
            }
        )
    }

    @Test
    fun eof() {
        setStringJson("")
        assertNextTokenIs("EOF")
    }

    @Test
    fun keyword() {
        setStringJson("hello")
        assertNextTokenIs("'hello'")
    }

    @Test
    fun string() {
        setStringJson("\"hello\"")
        assertNextTokenIs("string")
    }

    @Test
    fun float_number() {
        setStringJson("1.4545")
        assertNextTokenIs("1.4545")
    }

    @Test
    fun zero() {
        setStringJson("0")
        assertNextTokenIs("0")
    }

    @Test
    fun json_array() {
        setStringJson("[")
        assertNextTokenIs("array")
    }

    @Test
    fun json_object() {
        setStringJson("{")
        assertNextTokenIs("object")
    }

    @Test
    fun space() {
        setStringJson("_ ")
        reader.input.expect('_')
        assertNextTokenIs("whitespace (' ')")
    }

    @Test
    fun tab() {
        setStringJson("_\t")
        reader.input.expect('_')
        assertNextTokenIs("\\t")
    }

    @Test
    fun comma() {
        setStringJson(",")
        assertNextTokenIs("','")
    }

    @Test
    fun some_symbol() {
        setStringJson("^")
        assertNextTokenIs("'^'")
    }

    @Test
    fun ascii_control() {
        setStringJson("\u0003", skipWhitespace = false)
        assertNextTokenIs("\\u0003")
    }

    @Test
    fun long_string() {
        setStringJson("Very_long_string_should_be_cut_at_the_end. This substring should not be in result! 777")
        val got = buildString { appendNextJsonTokenFrom(reader.input) }
        assertFalse("777" in got)
        assertTrue("Very_long_string" in got)
        assertTrue(got.endsWith("...'"))
    }
}