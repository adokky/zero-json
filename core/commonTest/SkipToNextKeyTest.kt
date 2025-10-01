package dev.dokky.zerojson

import kotlinx.serialization.SerializationException
import kotlin.test.*

class SkipToNextKeyTest: AbstractJsonReaderTest() {
    @Test
    fun ok() {
        test("""{k1 :  "v" , k2 : v, k3 : -123 , k4 : 456.0 , k5 : false, k6 : null , k7 : [ 1, "2", { k:v } ] , k8 : { list : [ ] } }""") {
            expectBeginObject()
            val n = 8
            for (i in 1..n) {
                expectString("k$i")
                assertEquals(i < n, skipToNextKey(afterColon = false))
            }
            expectEndObject()
        }
    }

    @Test
    fun ok2() {
        test(":  656 , zzz}") {
            skipToNextKey(afterColon = false)
            expectString("zzz")
        }
    }

    @Test
    fun ok3() {
        test("656,zzz}") {
            skipToNextKey(afterColon = true)
            expectString("zzz")
        }
    }

    private fun assertFails(input: String, messageFragment: String? = null) {
        test(input) {
            val ex = assertFailsWith<SerializationException> { skipToNextKey(afterColon = false) }

            if (messageFragment != null) {
                val message = ex.message
                assertNotNull(message)
                assertContains(message, messageFragment)
            }
        }
    }

    @Test
    fun failures() {
        assertFails("",          "EOF")
        assertFails(" z",        "expected")
        assertFails(":",         "EOF")
        assertFails(": ",        "EOF")
        assertFails(":  v",      "EOF")
        assertFails(":  v ",     "EOF")
        assertFails(":  v ]",    "unexpected")
        assertFails(":  123",    "EOF")
        assertFails(":  123 ",   "EOF")
        assertFails(":  false",  "EOF")
        assertFails(":  false ", "EOF")
        assertFails(":  null",   "EOF")
        assertFails(":  null ",  "EOF")
        assertFails(":  null :", "unexpected")
        assertFails(":,",        "unexpected")
        assertFails(": ,",       "unexpected")
        assertFails(": , ",      "unexpected")
        assertFails(": , v",     "unexpected")
    }
}