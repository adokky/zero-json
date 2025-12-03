package dev.dokky.zerojson

import dev.dokky.zerojson.internal.StringBuilderWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class StringBuilderWrapperTest {
    @Test
    fun base_test() {
        val initialBuilder = StringBuilder()
        val w = StringBuilderWrapper(initialCapacity = 0, maxCapacity = 3, initialBuilder = initialBuilder)
        assertEquals(0, w.length)
        assertSame(initialBuilder, w.builder)

        assertEquals("xyz", w.buildString {
            append("xyz")
        })
        assertEquals(0, w.length)
        w.clearAndShrink()
        assertSame(initialBuilder, w.builder)

        assertEquals("a", w.buildString {
            append("a")
        })
        assertEquals(0, w.length)
        w.clearAndShrink()
        assertSame(initialBuilder, w.builder)

        assertEquals("abc", w.buildString {
            append("abc")
        })
        assertEquals(0, w.length)
        w.clearAndShrink()
        assertSame(initialBuilder, w.builder)

        assertEquals("1234", w.buildString {
            append("1234")
        })
        assertEquals(0, w.length)
        w.clearAndShrink()
        assertNotSame(initialBuilder, w.builder)

        w.builder.append("123")
        assertEquals(3, w.length)
    }

    @Test
    fun update_capacity_manually() {
        val initialBuilder = StringBuilder()
        val w = StringBuilderWrapper(maxCapacity = 3, initialBuilder = initialBuilder)

        w.builder.append("abcd")
        w.clearAndShrink()
        assertEquals(0, w.length)
        assertSame(initialBuilder, w.builder)

        w.builder.append("abcd")
        w.updateCapacity()
        w.clearAndShrink()
        assertNotSame(initialBuilder, w.builder)
        assertEquals(0, w.length)
    }

    @Test
    fun remove_sub_string() {
        val initialBuilder = StringBuilder()
        val w = StringBuilderWrapper(maxCapacity = 3, initialBuilder = initialBuilder)

        assertEquals("", w.removeSubstring(0))

        w.builder.append("123")
        assertEquals("123", w.removeSubstring(0))

        assertEquals("", w.removeSubstring(0))

        w.builder.append("1234")
        assertEquals("34", w.removeSubstring(2))

        assertEquals("12", w.toString())
    }

    @Test
    fun increasing_length() {
        val initialBuilder = StringBuilder()
        val w = StringBuilderWrapper(maxCapacity = 3, initialBuilder = initialBuilder)
        w.setLength(100)
        w.clearAndShrink()
        assertNotSame(initialBuilder, w.builder)
    }

    @Test
    fun decreasing_length() {
        val initialBuilder = StringBuilder()
        val w = StringBuilderWrapper(maxCapacity = 3, initialBuilder = initialBuilder)
        w.builder.append("12345")
        w.updateCapacity()
        w.setLength(2)
        assertEquals("12", w.toString())
        w.clearAndShrink()
        assertNotSame(initialBuilder, w.builder)
    }
}