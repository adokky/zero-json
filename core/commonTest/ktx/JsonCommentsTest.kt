@file:Suppress("JsonStandardCompliance")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonCommentsTest: JsonTestBase() {
    val json = ZeroJson {
        encodeDefaults = true
        allowComments = true
        allowTrailingComma = true
    }

    val withLenient = ZeroJson {
        encodeDefaults = true
        allowComments = true
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    @Test
    fun testBasic() = parametrizedTest { 
        val inputBlock = """{"data": "b" /*value b*/ }"""
        val inputLine = "{\"data\": \"b\" // value b \n }"
        assertEquals(StringData("b"), json.decodeFromStringTest(inputBlock))
        assertEquals(StringData("b"), json.decodeFromStringTest(inputLine))
    }

    @Serializable
    private data class StringData(val data: String)

    @Serializable
    private data class Target(val key: String, val key2: List<Int>, val key3: NestedTarget, val key4: String)

    @Serializable
    private data class NestedTarget(val nestedKey: String)

    private fun target(key4: String): Target = Target("value", listOf(1, 2), NestedTarget("foo"), key4)

    @Test
    fun testAllBlocks() = parametrizedTest { 
        val input = """{ /*beginning*/
            /*before key*/ "key" /*after key*/ : /*after colon*/ "value" /*before comma*/,
            "key2": [ /*array1*/ 1, /*array2*/ 2 /*end array*/],
            "key3": { /*nested obj*/ "nestedKey": "foo"} /*after nested*/,
            "key4": "/*comment inside quotes is a part of value*/"
            /*before end*/
        }"""
        assertEquals(target("/*comment inside quotes is a part of value*/"), json.decodeFromStringTest(input))
    }

    @Test
    fun testAllLines() = parametrizedTest { 
        val input = """{ //beginning
            //before key
            "key" // after key
             : // after colon
              "value" //before comma
              ,
            "key2": [ //array1
             1, //array2
              2 //end array
              ],
            "key3": { //nested obj
            "nestedKey": "foo"
            } , //after nested
            "key4": "//comment inside quotes is a part of value",
            //before end
        }"""
        assertEquals(target("//comment inside quotes is a part of value"), json.decodeFromStringTest(input))
    }

    @Test
    fun testMixed() = parametrizedTest { 
        val input = """{ // begin
           "key": "value", // after
            "key2": /* array */ /*another comment */ [1, 2],
            "key3": /* //this is a block comment */ { "nestedKey": // /*this is a line comment*/ "bar"
                "foo" },
            "key4": /* nesting block comments /* not supported */ "*/"
        /* end */}"""
        assertEquals(target("*/"), json.decodeFromStringTest(input))
    }

    @Test
    fun testWeirdKeys() {
        val map = mapOf(
            "// comment inside quotes is a part of key" to "/* comment inside quotes is a part of value */",
            "/*key */" to "/* value",
            "/* key" to "*/ value"
        )
        val input = """/* before begin */
            {
            ${map.entries.joinToString(separator = ",\n") { (k, v) -> "\"$k\" : \"$v\"" }}
            } // after end
        """.trimIndent()
        val afterMap = json.parseToJsonElement(input).jsonObject.mapValues { (_, v) ->
            v as JsonPrimitive
            assertTrue(v.isString)
            v.content
        }
        assertEquals(map, afterMap)
    }

    @Test
    fun testWithLenient() {
        parametrizedTest {
            val input = """{ //beginning
                //before key
                key // after key
                 : // after colon
                  value //before comma
                  ,
                key2: [ //array1
                 1, //array2
                  2 //end array
                  ],
                key3: { //nested obj
                nestedKey: "foo"
                } , //after nested
                key4: value//comment_cannot_break_value_apart, 
                key5: //comment without quotes where new token expected is still a comment
                value5,
                //before end
            }"""
            assertEquals(target("value//comment_cannot_break_value_apart"), withLenient.decodeFromStringTest(input))
        }
    }

    @Test
    fun testUnclosedCommentsErrorMsg() = parametrizedTest { 
        val input = """{"data": "x"} // no newline"""
        assertEquals(StringData("x"),  json.decodeFromStringTest<StringData>(input))
        val input2 = """{"data": "x"} /* no endblock"""
        assertFailsWith<SerializationException>("Expected end of the block comment: \"*/\", but had EOF instead at path: $") {
            json.decodeFromStringTest<StringData>(input2)
        }
    }

    private val lexerBatchSize = 16 * 1024

    @Test
    fun testVeryLargeComments() = parametrizedTest { 
        val strLen = lexerBatchSize * 2 + 42
        val inputLine = """{"data":  //a""" + "a".repeat(strLen) + "\n\"x\"}"
        assertEquals(StringData("x"),  json.decodeFromStringTest<StringData>(inputLine))
        val inputBlock = """{"data":  /*a""" + "a".repeat(strLen) + "*/\"x\"}"
        assertEquals(StringData("x"),  json.decodeFromStringTest<StringData>(inputBlock))
    }

    @Test
    fun testCommentsOnThresholdEdge() = parametrizedTest { 
        val inputPrefix = """{"data":  /*a"""
        // Here, we test the situation when closing */ is divided in buffer:
        // * fits in the initial buffer, but / is not.
        // E.g. situation with batches looks like this: ['{', '"', 'd', ..., '*'], ['/', ...]
        val bloatSize = lexerBatchSize - inputPrefix.length - 1
        val inputLine = inputPrefix + "a".repeat(bloatSize) + "*/\"x\"}"
        assertEquals(StringData("x"),  json.decodeFromStringTest<StringData>(inputLine))

        // Test when * is unclosed and last in buffer:
        val inputLine2 = inputPrefix + "a".repeat(bloatSize) + "*"
        assertFailsWith<SerializationException>("Expected end of the block comment: \"*/\", but had EOF instead at path: $") {
            json.decodeFromStringTest<StringData>(inputLine2)
        }
    }
}
