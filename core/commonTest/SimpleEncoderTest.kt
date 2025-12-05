package dev.dokky.zerojson

import dev.dokky.zerojson.framework.NumbersDataSet
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleEncoderTest: EncoderTest() {
    @Test
    fun simple() {
        test(
            """{"key":"UTF8-Строка"}""",
            SimpleDataClass("UTF8-Строка")
        )
    }

    @Test
    fun complex() {
        test(
            "{\"поле 1\":\"Привет, Мир!\"," +
            "\"int\":-2147483648," +
            "\"long\":9223372036854775807," +
            "\"nestedSimple\":{\"key\":\"simple\"}," +
            "\"selfNested\":{" +
                "\"поле 1\":\"Hello, World!\"," +
                "\"int\":2147483647," +
                "\"long\":-9223372036854775808," +
                "\"nullableString\":\"some \\\"string\\\"\"," +
                "\"nullableInt\":42," +
                "\"nullableLong\":42," +
                "\"nestedSimple\":{\"key\":\"\"}" +
            "}}",
            ComplexClass(
                `поле 1` = "Привет, Мир!",
                int = Int.MIN_VALUE,
                long = Long.MAX_VALUE,
                nullableString = null,
                nullableInt = null,
                nullableLong = null,
                nestedSimple = SimpleDataClass("simple"),
                selfNested = ComplexClass(
                    `поле 1` = "Hello, World!",
                    int = Int.MAX_VALUE,
                    long = Long.MIN_VALUE,
                    nullableString = "some \"string\"",
                    nullableInt = 42,
                    nullableLong = 42,
                    nestedSimple = SimpleDataClass(""),
                    selfNested = null
                ),
            )
        )
    }

    @Test
    fun explicit_nulls() {
        assertEquals(
            "{\"поле 1\":\"zzz\"," +
            "\"int\":2147483647," +
            "\"long\":-9223372036854775808," +
            "\"nullableString\":null," +
            "\"nullableInt\":null," +
            "\"nullableLong\":null," +
            "\"nestedSimple\":{\"key\":\"simple\"}," +
            "\"selfNested\":null}",
            TestZeroJson { explicitNulls = true }.encodeToString(
                ComplexClass(
                    `поле 1` = "zzz",
                    int = Int.MAX_VALUE,
                    long = Long.MIN_VALUE,
                    nullableString = null,
                    nullableInt = null,
                    nullableLong = null,
                    selfNested = null,
                    nestedSimple = SimpleDataClass("simple")
                )
            )
        )
    }

    private inline fun <reified T> test(nums: Iterable<T>) {
        for (n in nums) {
            test<T>(n.toString(), n)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun unsigned_numbers() {
        test(listOf<UByte>(0u, 1u, (UByte.MAX_VALUE - 1u).toUByte(), UByte.MAX_VALUE))
        test(listOf<UShort>(0u, 1u, (UShort.MAX_VALUE - 1u).toUShort(), UShort.MAX_VALUE))
        test(listOf<UInt>(0u, 1u, UInt.MAX_VALUE - 1u, UInt.MAX_VALUE))
        test(listOf<ULong>(0uL, 1uL, ULong.MAX_VALUE - 1uL, ULong.MAX_VALUE))
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun signed_numbers() {
        test(NumbersDataSet.ints8.toList())
        test(NumbersDataSet.ints16.toList())
        test(NumbersDataSet.ints32.toList())
        test(NumbersDataSet.ints64.toList())
    }

    @Test
    fun primitive_arrays() {
        test("""[]""", intArrayOf())
        test("""[123]""", intArrayOf(123))
        test("""[123,-456]""", intArrayOf(123, -456))
        test("""[123,-456,7]""", intArrayOf(123, -456, 7))
    }

    @Test
    fun lists() {
        test("""[]""", emptyList<Int>())

        test("""[1]""", listOf(1))

        test("""[1,234]""", listOf(1, 234))

        test("""["xxx","yyy"]""",
            listOf("xxx", "yyy")
        )

        test("""[{"xxx":"yyy"}]""",
            listOf(mapOf("xxx" to "yyy"))
        )

        test("""[{"abc":"def"},{"xxx":"yyy"}]""",
            listOf(mapOf("abc" to "def"), mapOf("xxx" to "yyy"))
        )

        test("""[{"key":"abc"}]""",
            listOf(SimpleDataClass("abc"))
        )

        test("""[{"key":"abc"},{"key":"xyz"}]""",
            listOf(
                SimpleDataClass("abc"),
                SimpleDataClass("xyz"),
            )
        )
    }

    @Test
    fun map0() = test("{}", mapOf<String, String>())

    @Test
    fun map1() = test("""{"xxx":"yyy"}""", mapOf("xxx" to "yyy"))

    @Test
    fun map2() = test("""{"a":"b","c":"d"}""", mapOf('a' to 'b', 'c' to 'd'))

    @Test
    fun map4() = test(
        """{"a":"b","c":"d","e":"f","g":"h"}""",
        mapOf('a' to 'b', 'c' to 'd', 'e' to 'f', 'g' to 'h')
    )

    @Test
    fun single_primitives() {
        test("\"entry 3\"", TestEnum.`entry 3`)
        test("true", true)
        test("-120", (-120).toByte())
        test("-120", (-120).toShort())
        test("143", 143)
        test("143", 143L)
        test("\"\\\"hello\\\"\"", "\"hello\"")
        test("\"Ё\"", "Ё")
        test<String?>("null", null)
        test<SimpleDataClass?>("null", null)
    }
}

