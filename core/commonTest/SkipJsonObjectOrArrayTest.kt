package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithMessage
import dev.dokky.zerojson.framework.format
import dev.dokky.zerojson.framework.generateRandomJsonArray
import dev.dokky.zerojson.framework.generateRandomJsonObject
import kotlinx.serialization.SerializationException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SkipJsonObjectOrArrayTest: AbstractJsonReaderTest() {
    private var fast = false

    private fun skipOA() {
        if (fast) reader.skipObjectOrArrayFast() else reader.skipObjectOrArray()
    }

    private fun skipOA(openingBracket: Char) {
        if (fast) reader.skipObjectOrArrayFast(openingBracket) else reader.skipObjectOrArray(openingBracket)
    }

    private val tokens = charArrayOf(':', ',', '}', ']', '[', '{')

    private fun test(string: String, openingBracket: Char? = null) {
        testExact(string, openingBracket)

        repeat(10) {
            val whiteSpaced = buildString(string.length * 2) {
                for (c in string) {
                    val wrapInSpace = c in tokens
                    if (wrapInSpace) append(if (Random.nextBoolean()) ' ' else '\n')
                    append(c)
                    if (wrapInSpace) append(if (Random.nextBoolean()) ' ' else '\t')
                }
            }
            testExact(whiteSpaced, openingBracket)
        }
    }

    private fun testExact(string: String, openingBracket: Char?) {
        test(string.trimStart()) {
            if (openingBracket != null) skipOA(openingBracket) else skipOA()
            expectEof()
        }
    }

    private fun assertFails(json: String) = assertFailsWith<SerializationException>(json)

    private inline fun <reified T: Throwable> assertFailsWith(json: String) = assertFailsWith<T> {
        test(json) { skipOA() }
    }

    private fun test() {
        test("{}1") {
            skipOA()
            assertEquals(1, reader.readInt())
        }

        // skipObjectOrArray expects an opening bracket
        assertFailsWith<AssertionError>("]")
        assertFailsWith<AssertionError>("}")

        assertFails("[")
        assertFails("{")
        assertFails("{]")
        assertFails("[}")

        test("[{}]")
        test("[{},{}]")
        test("[ { } ] ")
        test("[ { } , { } ] ")
        test("[] ]", openingBracket = '[')

        test("""{"k":"v","k":"v"},""") {
            skipOA()
            expectToken(',')
            expectEof()
        }

        repeat(1000) {
            val element = when {
                Random.nextInt(4) == 0 -> generateRandomJsonArray(maxDepth = 3)
                else -> generateRandomJsonObject(maxDepth = 2)
            }
            test(element.format(maxRandomSpaces = 3, unquoted = Random.nextBoolean()))
        }

        test("""{k:false,k:true,k:123,k:-67.0e2,k:null,k:[false,null,true,"",hello]}""")
        test("""{ k : false , k : true , k : 123 , k : -67.0e2 , k : null , k : [ false , null , true , "" , hello ] } """)

        assertFails("{")

        setBufferJson(" [] ]")
        assertFailsWith<SerializationException> { skipOA('{') }
    }

    @Test
    fun regular() {
        fast = false
        test()

        test("{[]]}") {
            assertFailsWithMessage<SerializationException>("expected string, got array") {
                skipOA()
            }
        }
    }

    @Test
    fun fast() {
        fast = true
        test()

        test("{  \"}\" : 1232345, {[ \"][ \\\" \\r\" ]}  } \"hello\"") {
            skipOA()
            assertEquals("hello", reader.readString())
        }

        test("{{[]}{}[]}123") {
            skipOA()
            assertEquals(123, reader.readInt())
        }

        test("[{{}}]")
        test("[{{{{{}}}}}]")

        assertFails("{[]]}")
        assertFails("{{[]}{}[]")
    }
}