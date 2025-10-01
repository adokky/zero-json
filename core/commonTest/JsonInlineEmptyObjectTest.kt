package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlin.test.Test

class JsonInlineEmptyObjectTest: RandomizedJsonTest() {
    @Serializable
    private data class SomeData(
        @JsonInline val inlined: InlinedData,
        val nullableInt: Int? = null
    )

    @Serializable
    private data class InlinedData(@JsonInline val map: Map<String, String>, val nullable: String?)

    @Serializable
    private data class SomeDataWithRequiredInlineFields(
        @JsonInline val inlined: RequiredInlinedData
    )

    @Serializable
    private data class RequiredInlinedData(@JsonInline val map: Map<String, String>, val string: String, val int: Int)

    @Test
    fun with_data() {
        randomizedTest {
            domainObject(
                SomeData(
                    InlinedData(
                        map = mapOf("abc" to "xyz"),
                        nullable = "hello"
                    ),
                    nullableInt = 42
                )
            )
            disable<RandomKeysInputTransformer>()
            jsonElement = jsonObject {
                "nullableInt" eq 42
                "abc" eq "xyz"
                "nullable" eq "hello"
            }
            iterations = 10
        }
    }

    @Test
    fun no_data() {
        randomizedTest {
            domainObject(
                SomeData(
                    InlinedData(
                        map = emptyMap(),
                        nullable = null
                    )
                )
            )
            disable<RandomKeysInputTransformer>()
            jsonElement = jsonObject {}
            iterations = 10
        }
    }

    @Test
    fun inline_field_required() {
        randomizedTest {
            domainObject(
                SomeDataWithRequiredInlineFields(
                    RequiredInlinedData(emptyMap(), "hello", 42)
                )
            )
            exclude(TestTarget.encoders())
            disable<RandomKeysInputTransformer>()
            disable<CorruptionInputTransformer>()
            expectFailure<MissingFieldException>(
                TestTarget.entries.filter { it.output == TestTarget.DataType.Domain },
                containsMessageSubString =
                    "Fields [string, int] are required for type with serial name " +
                    "'dev.dokky.zerojson.JsonInlineEmptyObjectTest.RequiredInlinedData'"
            )
            jsonElement = jsonObject {}
        }
    }
}