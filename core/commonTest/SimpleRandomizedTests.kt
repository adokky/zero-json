package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class SimpleRandomizedTests: RandomizedJsonTest() {
    @Test
    fun simple_data_class() = randomizedTestSuite {
        for (key in arrayOf("framework", "", "Привет, Мир!", "\"\\\\\" \\\n \\u0060")) {
            test("key='${key.jsonEscape()}'") {
                domainObject(SimpleDataClass(key))
                jsonElement { "key" eq key }
            }
        }
    }

    @Test
    fun enum() = randomizedTestSuite {
        for (entry in TestEnum.entries) {
            test(entry.toString()) {
                domainObject(entry)
                jsonElement = JsonPrimitive(entry.name)
                iterations = 10
            }
        }
    }

    @Test
    fun nested_data_class() {
        val value = ComplexClass(
            "framework",
            nullableString = null,
            nullableInt = 43535,
            int = Int.MIN_VALUE,
            long = Long.MIN_VALUE,
            nullableLong = null,
            nestedSimple = SimpleDataClass(""),
            selfNested = ComplexClass(
                "nested class",
                nullableInt = null,
                nullableString = null,
                int = Int.MAX_VALUE,
                long = Long.MAX_VALUE,
                nullableLong = 4535345353L,
                nestedSimple = SimpleDataClass("nested lv2"),
                selfNested = null
            )
        )

        randomizedTest {
            domainObject(value)
            jsonElement {
                "поле 1" eq value.`поле 1`
                "nullableString" eq value.nullableString
                "nullableInt" eq value.nullableInt
                "int" eq value.int
                "long" eq value.long
                "nullableLong" eq value.nullableLong
                "nestedSimple" {
                    "key" eq value.nestedSimple?.key
                }
                "selfNested" {
                    val nested = value.selfNested!!
                    "поле 1" eq nested.`поле 1`
                    "nullableString" eq nested.nullableString
                    "nullableInt" eq nested.nullableInt
                    "int" eq nested.int
                    "long" eq nested.long
                    "nullableLong" eq nested.nullableLong
                    "nestedSimple" {
                        "key" eq nested.nestedSimple?.key
                    }
                }
            }
        }
    }

    @Test
    fun lists1() {
        randomizedTest {
            domainObject(
                ArraysAndLists(
                    array = arrayOf(1, 2, 3),
                    intArray = intArrayOf(-1, -2, Int.MAX_VALUE),
                    intList = listOf(Int.MAX_VALUE, Int.MIN_VALUE, 0, -1, 1, 128)
                )
            )
            jsonElement {
                "array" (1, 2, 3)
                "intArray" (-1, -2, Int.MAX_VALUE)
                "intList" (Int.MAX_VALUE, Int.MIN_VALUE, 0, -1, 1, 128)
            }
        }
    }

    @Test
    fun lists2() {
        randomizedTest {
            domainObject(
                ArraysAndLists(
                    array = arrayOf(-100, -200, -300),
                    intArray = intArrayOf(),
                    intList = listOf(1, 2, 3, -4, 5, 6, 7, -8, 9)
                )
            )
            jsonElement {
                "array"(-100, -200, -300)
                "intArray" eq JsonArray(emptyList())
                "intList"(1, 2, 3, -4, 5, 6, 7, -8, 9)
            }
        }
    }

    @Test
    fun nulls_in_object() {
        randomizedTest {
            domainObject(ComplexClass(
                `поле 1` = "",
                int = 123,
                long = 456,
                nullableString = null,
                nullableInt = null,
                nullableLong = null,
                nestedSimple = null,
                selfNested = null,
            ))
            jsonElement {
                "поле 1" eq ""
                "int" eq 123
                "long" eq 456
                "nullableString" eq null
                "nullableInt" eq null
                "nullableLong" eq null
                "nestedSimple" eq null
                "selfNested" eq null
            }
        }
    }

    @Test
    fun unsigned_numbers() = randomizedTestSuite {
        val numbers = setOf<Number>(1L, -1L, 255L, -255L,
            Long.MIN_VALUE, Int.MIN_VALUE, Short.MIN_VALUE, Byte.MIN_VALUE,
            Byte.MAX_VALUE, Short.MAX_VALUE, Int.MAX_VALUE, Long.MAX_VALUE,
            ULong.MAX_VALUE.toLong(), ULong.MIN_VALUE.toLong()
        )

        for (num in numbers) {
            val n = num.toLong()
            val data = UnsignedNumbers(
                uByte = n.toUByte(),
                uInt = n.toUInt(),
                nuInt = null,
                uLong = n.toULong(),
                nuByte = null,
                uShort = n.toUShort(),
                nuShort = null,
                nuLong = null,
            )
            test(num.toString()) {
                domainObject(data)
                jsonElement {
                    "uByte" eq data.uByte
                    "uInt" eq data.uInt
                    "nuInt" eq data.nuInt
                    "uLong" eq data.uLong
                    "nuByte" eq data.nuByte
                    "uShort" eq data.uShort
                    "nuShort" eq data.nuShort
                    "nuLong" eq data.nuLong
                }
                iterations = 20
            }
        }
    }
}