@file:Suppress("ReplaceArrayOfWithLiteral")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNames
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonNamesTest : JsonTestBase() {

    @Serializable
    private data class WithNames(@JsonNames("foo", "_foo") val data: String)

    @Serializable
    private enum class AlternateEnumNames {
        @JsonNames("someValue", "some_value")
        VALUE_A,
        VALUE_B
    }

    @Serializable
    private data class WithEnumNames(
        val enumList: List<AlternateEnumNames>,
        val checkCoercion: AlternateEnumNames = AlternateEnumNames.VALUE_B
    )

    @Serializable
    private data class CollisionWithAlternate(
        @JsonNames("_foo") val data: String,
        @JsonNames("_foo") val foo: String
    )

    private val inputString1 = """{"foo":"foo"}"""
    private val inputString2 = """{"_foo":"foo"}"""

    @Test
    fun testEnumSupportsAlternativeNames() {
        val input = """{"enumList":["VALUE_A", "someValue", "some_value", "VALUE_B"], "checkCoercion":"someValue"}"""
        val expected = WithEnumNames(
            listOf(
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_A,
                AlternateEnumNames.VALUE_B
            ), AlternateEnumNames.VALUE_A
        )
        parametrizedTest {
            assertEquals(expected, default.decodeFromStringTest(input))
        }
    }

    @Test
    fun topLevelEnumSupportAlternativeNames() {
        parametrizedTest {
            assertEquals(AlternateEnumNames.VALUE_A, default.decodeFromStringTest("\"someValue\""))
        }
    }

    @Test
    fun testParsesAllAlternativeNames() {
        for (input in listOf(inputString1, inputString2)) {
            parametrizedTest {
                val data = default.decodeFromStringTest(WithNames.serializer(), input)
                assertEquals("foo", data.data, "Failed to parse input '$input'")
            }
        }
    }

    @Test
    fun testThrowsAnErrorOnDuplicateNames() {
        val serializer = CollisionWithAlternate.serializer()
        parametrizedTest {
            assertFailsWithMessage<SerializationException>(
                """Element with name '_foo' appeared twice in class with serial name 'dev.dokky.zerojson.ktx.JsonNamesTest.CollisionWithAlternate'""",
                "Class ${serializer.descriptor.serialName} did not fail"
            ) {
                default.decodeFromStringTest(serializer, inputString2)
            }
        }
    }
}
