package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.framework.assertFailsWith
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonNamingStrategyExclusionTest : JsonTestBase() {
    @SerialInfo
    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
    private annotation class OriginalSerialName

    private fun List<Annotation>.hasOriginal() = filterIsInstance<OriginalSerialName>().isNotEmpty()

    private val myStrategy = JsonNamingStrategy { descriptor, index, serialName ->
        if (descriptor.annotations.hasOriginal() || descriptor.getElementAnnotations(index).hasOriginal()) serialName
        else JsonNamingStrategy.SnakeCase.serialNameForJson(descriptor, index, serialName)
    }

    @Serializable
    @OriginalSerialName
    private data class Foo(val firstArg: String = "a", val secondArg: String = "b")

    private enum class E {
        @OriginalSerialName
        FIRST_E,
        SECOND_E
    }

    @Serializable
    private data class Bar(
        val firstBar: String = "a",
        @OriginalSerialName val secondBar: String = "b",
        val fooBar: Foo = Foo(),
        val enumBarOne: E = E.FIRST_E,
        val enumBarTwo: E = E.SECOND_E
    )

    private fun doTest(json: ZeroJson) {
        val j = ZeroJson(json) { namingStrategy = myStrategy }
        val bar = Bar()

        fun test(first: String, second: String) {
            parametrizedTest {
                val deserialized = j.decodeFromStringTest(
                    Bar.serializer(),
                    """{"first_bar":"a","secondBar":"b","foo_bar":{"firstArg":"a","secondArg":"b"},"enum_bar_one":"$first","enum_bar_two":"$second"}"""
                )
                assertEquals(bar, deserialized)
            }
        }

        test("FIRST_E", "second_e")

        assertFailsWith("ZeroJsonDecodingException", unwrap = true) { test("FIRST_E", "SECOND_E") }
        assertFailsWith("ZeroJsonDecodingException", unwrap = true) { test("first_e", "second_e") }
        assertFailsWith("ZeroJsonDecodingException", unwrap = true) { test("first_e", "SECOND_E") }
    }

    @Test
    fun testJsonNamingStrategyWithAlternativeNames() =
        doTest(ZeroJson(default) { useAlternativeNames = true })

    @Test
    fun testJsonNamingStrategyWithoutAlternativeNames() =
        doTest(ZeroJson(default) { useAlternativeNames = false })
}
