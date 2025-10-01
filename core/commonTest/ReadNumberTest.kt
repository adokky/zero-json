package dev.dokky.zerojson

import io.kodec.buffers.asBuffer
import kotlin.math.absoluteValue
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ReadNumberTest {
    @Test
    fun floats() {
        testFloatNumber("1.1")
        testFloatNumber("1.1e1")
        testFloatNumber("1.1e0")

        for (f in listOf(0.1, 1.0, 0.001, 100.0, 1234, 1234,5678)) {
            testFloatNumber(f.toString())
        }
    }

    @Test
    fun simpleIntegers() {
        for (n in (0..100)) {
            testIntNumber(n.toString())
            testIntNumber((-n).toString())
        }
    }

    @Test
    fun complexIntegers() {
        testIntNumber("0e1")
        testIntNumber("1e2")
        testIntNumber("2e1")

        testIntNumber("0e+2")
        testIntNumber("0e-2")

        testIntNumber("123e+2")

        assertFails { testIntNumber("123e-2") }
        assertFails { testIntNumber("1230e-2") }
        testIntNumber("12300e-2")

        testIntNumber("2e11")
        assertFails { testIntNumber("2e-11") }
    }

    private fun testFloatNumber(s: String) {
        val expected = s.toDouble()
        val actual = s.encodeToByteArray().asBuffer().let(JsonReader::startReadingFrom).readDouble()

        assertEquals(actual.sign, expected.sign)
        assertTrue((expected - actual).absoluteValue < 0.001)
    }

    private fun testIntNumber(s: String) {
        val expected = s.toDouble().roundToLong()
        val actual = s.encodeToByteArray().asBuffer().let(JsonReader::startReadingFrom).readLong()

        assertEquals(actual.sign, expected.sign)
        assertEquals(expected, actual)
    }
}