package dev.dokky.zerojson

import kotlinx.serialization.Serializable
import kotlin.test.Test

class JsonInlineEncoderMapTest: EncoderTest() {
    @Suppress("unused")
    @Serializable
    private class Parent(@JsonInline val child: ChildTop?, val string: String?, val int: Int)

    @Suppress("unused")
    @Serializable
    private class ChildTop(val name: String?, @JsonInline val inner: ChildInner?, val age: SimpleValueInteger?)

    @Suppress("unused")
    @Serializable
    private class ChildInner(@JsonInline val map: Map<SimpleValueClass, Int>)

    @Test
    fun case1() = test(
        """{"string":"hello","int":142}""",
        Parent(
            ChildTop(name = null, inner = ChildInner(mapOf()), age = null),
            string = "hello",
            int = 142
        )
    )

    @Test
    fun case2() = test(
        """{"name":"john","age":50,"string":"world","int":-1}""",
        Parent(
            ChildTop(name = "john", inner = null, age = SimpleValueInteger(50)),
            string = "world",
            int = -1
        )
    )

    @Test
    fun case3() = test(
        """{"int":-1}""",
        Parent(
            child = null,
            string = null,
            int = -1
        )
    )

    @Test
    fun case4() = test(
        """{"name":"xyz","a":100,"b":200,"c":300,"age":7856,"int":897}""",
        Parent(
            child = ChildTop(
                name = "xyz",
                inner = ChildInner(
                    mapOf(
                        SimpleValueClass("a") to 100,
                        SimpleValueClass("b") to 200,
                        SimpleValueClass("c") to 300
                    )
                ),
                age = SimpleValueInteger(7856)
            ),
            string = null,
            int = 897
        )
    )
}