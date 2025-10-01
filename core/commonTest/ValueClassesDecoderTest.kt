package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import kotlin.test.Test

class ValueClassesDecoderTest: AbstractDecoderTest() {
    @Test
    fun case1() = encodeDecode(42.toUInt())

    @Test
    fun case2() = encodeDecode(SimpleValueClass("класс-значение"))

    @Test
    fun case3() = encodeDecode(mapOf(SimpleValueClass("ключ") to SimpleValueClass("значение")))

    @Test
    fun case4() = encodeDecode(mapOf(SimpleValueClassWrapper("ключ") to SimpleValueClassWrapper("значение")))

    @Test
    fun double_wrapper_case1() = encodeDecode(
        NullableValueClassDoubleWrapper(
            NullableValueClassWrapper(
                SimpleValueClass("СТРОКА")
            )
        )
    )

    @Test
    fun double_wrapper_case3() = encodeDecode(
        NullableValueClassDoubleWrapper(null)
    )
}