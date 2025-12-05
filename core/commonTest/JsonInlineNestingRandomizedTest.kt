@file:Suppress("PropertyName", "ClassName")

package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test

@ExperimentalSerializationApi
class JsonInlineNestingRandomizedTest: RandomizedJsonTest() {
    @Serializable
    private data class Root(
        val string: String,
        @JsonInline val c11: C1_1,
        val list: List<String?>?,
        @JsonInline val c12: C1_2,
        val int: Int?
    )

    @Serializable
    private data class C1_1(val c11_name: String?, val c11_age: Int, @JsonInline val c21: C2_1?)

    @Serializable
    private data class C1_2(val c12_name: String, @JsonInline val c22: C2_2?, val c12_age: Int?)

    @Serializable
    private data class C2_1(val c21_name: String?, val c21_age: Int)

    @Serializable
    private data class C2_2(@JsonInline val c3: C3?, val c22_name: String, val c22_age: Int?)

    @Serializable
    private data class C3(val c3_name: String?, val c3_age: Int, @JsonInline val map: Map<Int, SimpleValueClass>)

    @Test
    fun randomized1() {
        val inlinedMap = mapOf(
            900 to SimpleValueClass("значение 1"),
            -900 to SimpleValueClass("ЗНАЧЕНИЕ 2"),
        )

        val root = Root(
            string = "a string",
            c11 = C1_1(null, 570, C2_1(null, 123)),
            c12 = C1_2("zzz", C2_2(C3(null, 45, inlinedMap), "", null), null),
            list = listOf("hello", " ", "world!"),
            int = -228
        )

        randomizedTest {
            domainObject(root)
            disable<RandomKeysInputTransformer>()
            jsonElement {
                "string" eq root.string
                "c11_name" eq root.c11.c11_name
                "c12_name" eq root.c12.c12_name
                "c21_name" eq root.c11.c21!!.c21_name
                "c22_name" eq root.c12.c22!!.c22_name
                "c3_age" eq root.c12.c22.c3!!.c3_age
                "c3_name" eq root.c12.c22.c3.c3_name
                "c11_age" eq root.c11.c11_age
                "c12_age" eq root.c12.c12_age
                "c21_age" eq root.c11.c21.c21_age
                "c22_age" eq root.c12.c22.c22_age
                "900" eq inlinedMap.getValue(900).key
                "-900" eq inlinedMap.getValue(-900).key
                "list" stringArray root.list!!
                "int" eq root.int
            }
        }
    }

    @Test
    fun randomized2() {
        val root = Root(
            string = "a string",
            c11 = C1_1(c11_name = null, c11_age = 570, c21 = C2_1(null, 123)),
            c12 = C1_2(
                c12_name = "zzz",
                c22 = C2_2(c3 = null, c22_name = "", c22_age = null),
                c12_age = null
            ),
            list = listOf(null, "", null),
            int = -228
        )

        randomizedTest {
            domainObject(root)
            disable<RandomKeysInputTransformer>()
            jsonElement {
                "string" eq root.string
                "c11_name" eq root.c11.c11_name
                "c12_name" eq root.c12.c12_name
                "c21_name" eq root.c11.c21!!.c21_name
                "c22_name" eq root.c12.c22!!.c22_name
                "c11_age" eq root.c11.c11_age
                "c12_age" eq root.c12.c12_age
                "c21_age" eq root.c11.c21.c21_age
                "c22_age" eq root.c12.c22.c22_age
                "list" stringArray root.list!!
                "int" eq root.int
            }
        }
    }

    @Test
    fun invalid_inline_map_entry_key() {
        assertFailsWithSerialMessage("expected integer, got '11_name'") {
            println(
                TestZeroJson { ignoreUnknownKeys = false }.decodeFromString<Root>(
                    """{
                        |string:"a string",
                        |11_name:nll,
                        |c12_name:zzz,
                        |c21_name:null,
                        |c22_name:"",
                        |c11_age:570,
                        |c12_age:null,
                        |c21_age:123,
                        |c22_age:null,
                        |list:[null,"",null],
                        |int:-228
                     }""".trimMargin()
                )
            )
        }
    }
}

