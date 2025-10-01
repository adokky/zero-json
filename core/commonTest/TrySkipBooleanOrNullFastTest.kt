package dev.dokky.zerojson

import dev.dokky.zerojson.internal.trySkipBooleanOrNullFast
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrySkipBooleanOrNullFastTest: AbstractJsonReaderTest() {
    @Test
    fun success() {
        test("true") { assertTrue(input.trySkipBooleanOrNullFast()) }
        test("false") { assertTrue(input.trySkipBooleanOrNullFast()) }
        test("null") { assertTrue(input.trySkipBooleanOrNullFast()) }
    }

    @Test
    fun failure() {
        fun test(input: String) = test(input) { assertFalse(reader.input.trySkipBooleanOrNullFast()) }

        test("truee")
        test("falsee")
        test("nulll")

        test("trueБ")
        test("falseБ")
        test("nullБ")

        test("ttrue")
        test("ffalse")
        test("nnull")

        test("t${(('t'.code shl 8) or 'r'.code).toChar()}ue")

        test("True")
        test("False")
        test("Null")

        for (shift in 7..14) {
            test(charArrayOf('n', 'u', 'l'.code.or(1 shl shift).toChar(), 'l').concatToString())
        }
    }
}