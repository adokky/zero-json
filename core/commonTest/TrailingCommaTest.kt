package dev.dokky.zerojson

import dev.dokky.zerojson.framework.RandomizedJsonTest
import dev.dokky.zerojson.framework.assertFailsWithSerialMessage
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

// This test is kinda useless now because most test cases using randomized testing framework,
// which has TrailingCommaInputTransformer enabled by default.
class TrailingCommaTest: RandomizedJsonTest() {
    private val zJsonTcDisabled = TestZeroJson { allowTrailingComma = false }
    private val zJsonTcEnabled = TestZeroJson { allowTrailingComma = true }

    private inline fun <reified T> test(data: T, jsonInput: String) {
        assertFailsWithSerialMessage("trailing") {
            zJsonTcDisabled.decodeFromString<T>(jsonInput)
        }

        assertEquals(data, zJsonTcEnabled.decodeFromString<T>(jsonInput))
    }

    @Test
    fun simple_data_class() {
        test(
            SimpleDataClass("framework"),
            """{"key":"framework",}"""
        )
    }

    @Test
    fun map() {
        test(
            mapOf(123 to "value"),
            """{"123":"value",}"""
        )
        test(
            mapOf(123 to "value", 456 to ""),
            """{"123":"value",456:"",}"""
        )
    }

    @Test
    fun array() {
        test(listOf(1), """[1,]""")
        test(listOf(1, 2), """[1,2,]""")
    }

    @Test
    fun polymorphism() {
        val value = PolyInterface.SubClass1.Concrete2("123", 345)
        val serialName = PolyInterface.SubClass1.Concrete2::class.qualifiedName
        test<PolyInterface>(value, """{"type":"$serialName","string":"123","int":345,}""")
        test<PolyInterface>(value, """{"string":"123","type":"$serialName","int":345,}""")
        test<PolyInterface>(value, """{"string":"123","int":345,"type":"$serialName",}""")
    }

    @Test
    fun single_comma_should_fail() {
        fun test(s: String) {
            for (zjson in listOf(zJsonTcDisabled, zJsonTcEnabled)) {
                val ex = assertFailsWith<SerializationException> {
                    zjson.decodeFromString<Map<String, String>>(s)
                }
                assertNotEquals(true, ex.message?.contains("trailing"))
            }
        }
        test("{,}")
        test("[,]")
    }
}