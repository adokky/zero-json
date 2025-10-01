package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Suppress("OPT_IN_USAGE")
class JsonInlineRandomizedTest: RandomizedJsonTest() {
    @Serializable
    private data class Parent(val string: String, @JsonInline val inline: Child, val int: Int)

    @Serializable
    private data class Child(val name: String, val age: Int)

    @Test
    fun simple() {
        val obj = Parent(
            string = "hello",
            int = 150,
            inline = Child(name = "Alex", age = 20)
        )

        randomizedTest {
            domainObject(obj)
            disable<RandomKeysInputTransformer>()
            jsonElement {
                "name" eq obj.inline.name
                "string" eq obj.string
                "age" eq obj.inline.age
                "int" eq obj.int
            }
        }
    }

    @Suppress("EnumEntryName")
    @Serializable
    private enum class MyEnum { abc, def, ghi }

    @Serializable
    private data class Level0(val enum: MyEnum = MyEnum.abc, val nested: Level1)

    @Serializable
    private data class Level1(val nested1: Level2, val nested2: Level2?, val enum: MyEnum?, val name: String)

    @Serializable
    private data class Level2(@JsonInline val inlined: Level3)

    @Serializable
    private data class Level3(
        val name: String,
        val nested: Level4?,
        @JsonInline val inlined: Level4?,
        val age: Int,
        val enumTop: MyEnum?,
        @JsonInline val map: Map<String, MyEnum>?
    )

    @Serializable
    private data class Level4(val enumNested: MyEnum?, val int: Int)

    @Test fun complex() = doTest(coercing = false)

    @Test fun complex_with_coercing() = doTest(coercing = true)

    private fun doTest(coercing: Boolean) {
        val obj = Level0(
            nested = Level1(
                enum = null,
                name = "$$*/",
                nested1 = Level2(
                    inlined = Level3(
                        name = "dfd",
                        nested = Level4(MyEnum.def, 7657),
                        inlined = Level4(MyEnum.abc, 7657),
                        age = 454,
                        enumTop = MyEnum.ghi,
                        map = null
                    )
                ),
                nested2 = Level2(
                    inlined = Level3(
                        name = "inner object",
                        nested = null,
                        inlined = null,
                        age = 909,
                        enumTop = MyEnum.abc,
                        map = mapOf("abc" to MyEnum.def, "def" to MyEnum.ghi)
                    )
                )
            )
        )

        randomizedTest {
            domainObject(obj)
            json = ZeroJson { coerceInputValues = coercing }
            // can not be auto tested because of default values and implicit nulls
            excludeTargetIf { it.input == TestTarget.DataType.Domain }
            disable<RandomKeysInputTransformer>()
            jsonElement {
                "enum" eq (if (coercing) "invalid_entry" else MyEnum.abc.toString())
                "nested" {
                    "enum" eq (if (coercing) "xyz" else null)
                    "name" eq obj.nested.name
                    "nested1" { // Level2 = inlined Level3
                        "name" eq obj.nested.nested1.inlined.name
                        "nested" {
                            "enumNested" eq obj.nested.nested1.inlined.nested?.enumNested
                            "int" eq obj.nested.nested1.inlined.nested?.int
                        }
                        "enumNested" eq obj.nested.nested1.inlined.inlined?.enumNested
                        "int" eq obj.nested.nested1.inlined.inlined?.int
                        "age" eq obj.nested.nested1.inlined.age
                        "enumTop" eq obj.nested.nested1.inlined.enumTop
                    }
                    "nested2" { // Level2 = inlined Level3
                        "name" eq obj.nested.nested2?.inlined?.name
                        "nested" eq null
                        "age" eq obj.nested.nested2?.inlined?.age
                        "enumTop" eq obj.nested.nested2?.inlined?.enumTop
                        "abc" eq obj.nested.nested2?.inlined?.map?.getValue("abc")
                        "def" eq obj.nested.nested2?.inlined?.map?.getValue("def")
                        if (coercing) "opq" eq "xyz"
                    }
                }
            }
        }
    }
}