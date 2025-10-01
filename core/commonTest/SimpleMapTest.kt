package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import kotlin.test.Test

class SimpleMapTest: AbstractDecoderTest() {
    @Test
    fun map_with_non_string_key_1() = encodeDecode(mapOf<Int, String>())

    @Test
    fun map_with_non_string_key_2() = encodeDecode(mapOf<Int, String>(23 to "xyz"))

    @Test
    fun map_with_non_string_key_3() = encodeDecode(mapOf<Int, String>(
        12 to "xyz",
        23 to "abc",
        -45 to "def",
        67 to "ghi",
        -89 to "Just a long string"
    ))

    @Test
    fun map_of_strings_1() = encodeDecode(mapOf<String, String>())

    @Test
    fun map_of_strings_2() = encodeDecode(mapOf<String, String>("a" to "1"))

    @Test
    fun map_of_strings_3() {
        for (size in arrayOf(2, 3, 4, 5, 10, 300)) {
            val value = (1 .. size).associate {
                ('a'.code + it).toChar().toString() to it.toString()
            }
            encodeDecode(value)
        }
    }

    @Test
    fun maps_inside_holder() {
        encodeDecode(Maps(
            map1 = mapOf(1 to 2, 3 to 34543),
            map2 = mapOf(
                "inner1" to Maps(mapOf(Int.MAX_VALUE to Int.MIN_VALUE)),
                "inner2" to Maps(emptyMap()),
            ),
            inner = Maps(mapOf(11 to 22, 33 to 44, 55 to 66))
        ))
    }

    @Test
    fun enum_in_map() {
        encodeDecode(mapOf<TestEnum, TestEnum>())
        encodeDecode(mapOf(
            TestEnum.entry1 to TestEnum.entry2
        ))
        encodeDecode(mapOf(
            TestEnum.entry2 to TestEnum.entry1,
            TestEnum.`entry 3` to TestEnum.entry1
        ))
        encodeDecode(mapOf(
            TestEnum.entry2 to TestEnum.entry1,
            TestEnum.entry1 to TestEnum.entry1,
            TestEnum.`entry 3` to TestEnum.entry1
        ))
    }
}