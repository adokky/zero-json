package dev.dokky.zerojson

import dev.dokky.zerojson.framework.NumbersDataSet
import dev.dokky.zerojson.framework.testIterations
import dev.dokky.zerojson.internal.readNumberAsString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadNumberAsStringTest: AbstractJsonReaderTest() {
    private val sb = StringBuilder()

    @Test
    fun ints() {
        check("1234567")
        check("00001")
        check("00001e")
        check("00001e1")
        check("12345e67")

        checkFails("")
        checkFails(" ")
        checkFails("hello")

        for (n in NumbersDataSet.ints64) {
            check(n.toString())
            check((-n).toString())
        }
    }

    @Test
    fun floats() {
        check("0.")
        check("1.2")
        check("00001.123e1234")
        check("3.e67")

        checkFails("3.e")
        checkFails("3.4e")

        for (n in NumbersDataSet.getFloat64().take(testIterations(10_000, 1_000_000))) {
            if (!n.isFinite()) continue
            check(n.toString())
        }
    }

    private fun check(expected: String) {
        test(expected) { assertEquals(expected, readNumberAsString(sb)) }
        test(expected + ":") { assertEquals(expected, readNumberAsString(sb)) }
        checkFails("z" + expected)
        checkFails(expected + "z")
    }

    private fun checkFails(input: String) {
        test(input) { assertNull(readNumberAsString(sb)) }
    }
}