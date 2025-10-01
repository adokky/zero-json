package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("OPT_IN_USAGE")
class JsonInlineSimpleDecoderTest: AbstractDecoderTest() {
    @Serializable
    private data class Parent(val string: String, @JsonInline val inline: Child, val int: Int)

    @Serializable
    private data class Child(val name: String, val age: Int)

    private val simpleParent = Parent(
        string = "hello",
        int = 150,
        inline = Child(name = "Alex", age = 20)
    )

    @Test
    fun simple_fast_path() {
        val json = """
            {
                "string": "${simpleParent.string}",
                "name": "${simpleParent.inline.name}",
                "age": ${simpleParent.inline.age},
                "int": ${simpleParent.int}
            }
        """.trimIndent()

        val actual = ZeroJson.decode<Parent>(json)

        assertEquals(simpleParent, actual)
    }

    @Serializable
    private data class MapFast(val name: String, @JsonInline val map: Map<String, Int>, val age: Int)

    @Test
    fun map_fast_path() {
        val expected = MapFast(
            name = "Простая строка",
            age = 7,
            map = mapOf(
                "age3" to 5678,
                "age2" to 1234
            )
        )

        val json = """
            {
                "name": "${expected.name}",
                "age2": ${expected.map["age2"]},
                "age": ${expected.age},
                "age3": ${expected.map["age3"]}
            }
        """.trimIndent()

        val actual = ZeroJson.decode<MapFast>(json)

        assertEquals(expected, actual)
    }

    @Serializable
    private data class MapSlow(
        val string: String,
        @JsonInline val inline: Child,
        @JsonInline val map: Map<String, Int>,
        val num: Int
    )

    private fun testMapSlowPath(json: MapSlow.() -> String) {
        val expected = MapSlow(
            string = "Простая строка",
            num = 7,
            inline = Child("A String", -100),
            map = mapOf(
                "age3" to 5678,
                "age2" to 1234
            )
        )

        val json = expected.json()

        val actual = ZeroJson.decode<MapSlow>(json)

        assertEquals(expected, actual)
    }

    @Test
    fun map_slow_path_1() {
        testMapSlowPath {
            """
            {
                "string": "$string",
                "name": "${inline.name}",
                "age2": ${map["age2"]},
                "num": $num,
                "age": ${inline.age},
                "age3": ${map["age3"]}
            }
            """.trimIndent()
        }
    }

    @Test
    fun map_slow_path_2() {
        testMapSlowPath {
            """
            {
                "age2": ${map["age2"]},
                "name": "${inline.name}",
                "string": "$string",
                "age": ${inline.age},
                "age3": ${map["age3"]},
                "num": $num
            }
            """.trimIndent()
        }
    }
}