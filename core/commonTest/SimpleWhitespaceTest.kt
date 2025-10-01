package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import dev.dokky.zerojson.framework.GlobalTestMode
import dev.dokky.zerojson.framework.TestMode
import kotlin.test.Test
import kotlin.test.assertEquals

class SimpleWhitespaceTest: AbstractDecoderTest() {
    private val spaces = listOf("", " ", "  ", "\n", "\r\n")

    private var iteration = 0
    @Suppress("PrivatePropertyName")
    private val S get() = if (iteration in spaces.indices) spaces[iteration] else spaces.random()

    @Test
    fun whitespaces() {
        repeat(if (GlobalTestMode == TestMode.QUICK) 100 else 10_000) { i ->
            iteration = i

            @Suppress("ZeroJsonStandardCompliance")
            val string =
                "$S{$S\"поле 1\"$S:$S\"Привет, Мир!\"$S,$S" +
                "$S\"int\"$S:$S-2147483648$S,$S" +
                "$S\"long\"$S:${S}9223372036854775807$S,$S" +
                "$S\"nestedSimple\"$S:$S{$S\"key\"$S:$S\"simple\"$S}$S,$S" +
                "$S\"selfNested\"$S:$S{$S" +
                "$S\"поле 1\"$S:$S\"Hello, World!\"$S,$S" +
                "$S\"int\"$S:${S}2147483647$S,$S" +
                "$S\"long\"$S:$S-9223372036854775808$S,$S" +
                "$S\"nullableString\"$S:$S\"some \\\"string\\\"\"$S,$S" +
                "$S\"nullableInt\"$S:${S}42${S},${S}" +
                "$S\"nullableLong\"$S:${S}42$S,$S" +
                "$S\"nestedSimple\"$S:${S}{$S\"key\":\"\"$S}$S" +
                "$S}$S}$S"

            assertEquals(
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
                ),
                zjson.decode(string),
                string
            )
        }
    }
}