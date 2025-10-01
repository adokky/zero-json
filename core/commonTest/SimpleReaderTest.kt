package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.ZeroStringTextReader
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleReaderTest {
    private val input = ZeroStringTextReader()
    private val reader = JsonReaderImpl(input)

    private fun check(s: String, expected: ZeroJsonElement?) {
        input.startReadingFrom(s)
        reader.skipWhitespace()
        assertEquals(expected, reader.readValue())
    }

    @Test
    fun int32() {
        check("0", 0)
        check("-1", -1)
        check("-01", -1)
        check("023", 23)
        check("-0e1", 0)
        check("4e1", 40)
        check("-1e3", -1000)
        check("56e3", 56000)
        check("-3568942", -3568942)
        check("3568942", 3568942)
        check(Int.MIN_VALUE.toString(), Int.MIN_VALUE)
        check(Int.MAX_VALUE.toString(), Int.MAX_VALUE)
    }

    @Test
    fun int64() {
        check("3457700025111", 3457700025111)
        check("-3455878342111", -3455878342111L)
        check(Long.MIN_VALUE.toString(), Long.MIN_VALUE)
        check(Long.MAX_VALUE.toString(), Long.MAX_VALUE)
    }

    @Test
    fun uint64() {
        (ULong.MAX_VALUE - 1000.toULong()).also {
            check(it.toString(), it)
        }
        check(ULong.MAX_VALUE.toString(), ULong.MAX_VALUE)
    }

    @Test
    fun booleans() {
        check("true", true)
        check("false", false)
    }

    @Test
    fun strings() {
        check("\"\"", "")
        check("\"true\"", "true")
        check("\"false\"", "false")

        check("False", "False")
        check("True", "True")

        for (s in listOf(
            "T", "П", "Hello", "Привет.Мир!"
        )) {
            check(s, s)
            check("$s string", s)
            for (c in charArrayOf(' ', '{', '}', '[', ']', ',', '"', ':', '\n', '\t'))
                check(s + c, s)
        }

        check("\"Hello, world!\"", "Hello, world!")
    }

    @Test
    fun arrays() {
        check("[]", listOf<ZeroJsonElement>())
        check(
            "[\"\", true, false, -235, 23.0, null, [1, \",Hello,\", 2]]",
            listOf("", true, false, -235, 23.0, null, listOf(1, ",Hello,", 2))
        )
    }

    @Test
    fun objects() {
        check("{}", mapOf<String, ZeroJsonElement>())

        check(
            """
                {
                    "key 1": 1,
                    "key 2": -13434,
                    "key 3": 70.0,
                    "key 4": true,
                    "key 5": false,
                    "key 6": "another string",
                    "key 7": 4003460000,
                    "nested": {
                        "ключ": "значение"
                    },
                    "nested list": [1, 2, 3]
                }
            """.trimIndent(),
            mapOf<String, ZeroJsonElement>(
                "key 1" to 1,
                "key 2" to -13434,
                "key 3" to 70.0,
                "key 4" to true,
                "key 5" to false,
                "key 6" to "another string",
                "key 7" to 4003460000L,
                "nested" to mapOf<String, ZeroJsonElement>("ключ" to "значение"),
                "nested list" to listOf(1, 2, 3)
            )
        )
    }
}