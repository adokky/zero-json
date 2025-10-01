package dev.dokky.zerojson

import io.kodec.text.readStringUntil
import kotlin.test.Test
import kotlin.test.assertEquals

class SkipJsonWhiteSpaceTest: AbstractJsonReaderTest() {
    @Test
    fun skipWhitespace1() {
        test("\t\r\n a b") {
            skipWhitespace()
            assertEquals("a", input.readStringUntil(ending = ' '))
            skipWhitespace()
            assertEquals("b", input.readStringUntil(ending = ' '))
            skipWhitespace()
        }

    }

    @Test
    fun skipWhitespace2() {
        test("hello") {
            assertEquals("hello", input.readStringUntil(ending = ' '))
            skipWhitespace()
        }
    }
}