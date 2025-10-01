@file:Suppress("PropertyName", "ClassName")

package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test

@ExperimentalSerializationApi
class JsonInlineNestedTest: RandomizedJsonTest() {
    @Serializable
    private data class Root(
        val string: String,
        @JsonInline val c11: C1_1,
        val list: List<String?>?,
        @JsonInline val c12: C1_2,
        val int: Int?
    )

    @Serializable
    private data class C1_1(val c11_name: String?, val c11_age: Int, @JsonInline val c21: C2_1)

    @Serializable
    private data class C1_2(val c12_name: String, @JsonInline val c22: C2_2, val c12_age: Int?)

    @Serializable
    private data class C2_1(val c21_name: String?, val c21_age: Int)

    @Serializable
    private data class C2_2(@JsonInline val c3: C3, val c22_name: String, val c22_age: Int?)

    @Serializable
    private data class C3(val c3_name: String?, val c3_age: Int)

    @Test
    fun randomized() {
        val root = Root(
            string = "a string",
            c11 = C1_1(null, 570, C2_1(null, 123)),
            c12 = C1_2("zzz", C2_2(C3(null, 45), "", null), null),
            list = listOf(null, "", null),
            int = -228
        )

        randomizedTest {
            domainObject(root)
            disable<RandomKeysInputTransformer>()
            iterations = 100
            jsonElement {
                "string" eq root.string
                "c11_name" eq root.c11.c11_name
                "c12_name" eq root.c12.c12_name
                "c21_name" eq root.c11.c21.c21_name
                "c22_name" eq root.c12.c22.c22_name
                "c3_age" eq root.c12.c22.c3.c3_age
                "c3_name" eq root.c12.c22.c3.c3_name
                "c11_age" eq root.c11.c11_age
                "c12_age" eq root.c12.c12_age
                "c21_age" eq root.c11.c21.c21_age
                "c22_age" eq root.c12.c22.c22_age
                "list"(null, "", null)
                "int" eq root.int
            }
        }
    }
}