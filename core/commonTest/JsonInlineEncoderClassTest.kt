package dev.dokky.zerojson

import kotlinx.serialization.Serializable
import kotlin.test.Test

class JsonInlineEncoderClassTest: EncoderTest() {
    @Suppress("unused")
    @Serializable
    private class Parent(
        @JsonInline val child: ChildTop? = null,
        val string: String? = null,
        val int: Int,
        @JsonInline val last: ChildLast? = null,
    )

    @Suppress("unused")
    @Serializable
    private class ChildTop(
        val name: String? = null,
        @JsonInline val inner: ChildInner? = null,
        val age: SimpleValueInteger? = null
    )

    @Suppress("unused")
    @Serializable
    private class ChildInner(val list: List<String>)

    @Suppress("unused")
    @Serializable
    private class ChildLast(val num: Int, val prop: String)

    @Test
    fun case1() = test(
        """{"list":[],"string":"hello","int":898}""",
        Parent(
            ChildTop(inner = ChildInner(emptyList())),
            string = "hello",
            int = 898
        )
    )

    @Test
    fun case2() = test(
        """{"name":"john","age":50,"string":"world","int":-1}""",
        Parent(
            ChildTop(name = "john", age = SimpleValueInteger(50)),
            string = "world",
            int = -1
        )
    )

    @Test
    fun case3() = test(
        """{"int":-1}""",
        Parent(int = -1)
    )

    @Test
    fun case4() = test(
        """{"name":"xyz","list":["a","b","c"],"age":7856,"int":897}""",
        Parent(
            child = ChildTop(
                name = "xyz",
                inner = ChildInner(listOf("a", "b", "c")),
                age = SimpleValueInteger(7856)
            ),
            int = 897
        )
    )

    @Test
    fun case5() = test(
        """{"list":[],"string":"hello","int":123,"num":-557,"prop":"last string"}""",
        Parent(
            ChildTop(inner = ChildInner(emptyList())),
            string = "hello",
            int = 123,
            last = ChildLast(num = -557, prop = "last string")
        )
    )
}