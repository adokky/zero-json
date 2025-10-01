package dev.dokky.zerojson

import dev.dokky.zerojson.internal.toOpeningBracket
import kotlin.test.Test
import kotlin.test.assertEquals

class AsciiUtilsTest {
    @Test
    fun is_bracket() {
        for (c in 0..0xf00) {
            var isOpening = false
            var isClosing = false
            when (c) {
                '{'.code, '['.code -> isOpening = true
                '}'.code, ']'.code -> isClosing = true
            }
            assertEquals(isOpening, c.isOpeningBracket())
            assertEquals(isClosing, c.isClosingBracket())
        }
    }

    @Test
    fun counter_bracket() {
        assertEquals('[', ']'.code.toOpeningBracket().toChar())
        assertEquals('{', '}'.code.toOpeningBracket().toChar())

        assertEquals(']', '['.code.toClosingBracket().toChar())
        assertEquals('}', '{'.code.toClosingBracket().toChar())
    }
}