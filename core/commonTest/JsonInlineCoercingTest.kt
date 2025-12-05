package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsonInlineCoercingTest: RandomizedJsonTest() {
    @Serializable
    private data class Root(
        @JsonInline val inlined: Inlined,
        val int: Int
    )

    @Serializable
    private data class Inlined(
        val inlinedString: String = "4554",
        val inlinedEnum1: TestEnum = TestEnum.`entry 3`,
        val inlinedEnum2: TestEnum?,
        @JsonInline val map: Map<String, TestEnum> = mapOf()
    )

    private val coercingJson = TestZeroJson { coerceInputValues = true }

    @Test
    fun no_coercing() {
        val obj = Root(
            inlined = Inlined(
                inlinedString = "zzz",
                inlinedEnum1 = TestEnum.`entry 3`,
                inlinedEnum2 = null,
                map = mapOf(
                    "abc" to TestEnum.entry1,
                    "def" to TestEnum.entry2,
                    "ghi" to TestEnum.entry2,
                    "xyz" to TestEnum.`entry 3`
                )
            ),
            int = 101
        )

        randomizedTest {
            domainObject(obj)
            exclude(TestTarget.ObjectToText)
            disable<RandomKeysInputTransformer>()
            jsonElement {
                "int" eq obj.int
                "inlinedString" eq obj.inlined.inlinedString
                "inlinedEnum2" eq obj.inlined.inlinedEnum2
                for ((k, v) in obj.inlined.map) {
                    k eq v
                }
            }
        }
    }

    @Test
    fun coerced1() {
        val obj = Root(
            inlined = Inlined(
                inlinedEnum2 = null,
                map = mapOf("sdd" to TestEnum.entry2)
            ),
            int = 34
        )

        randomizedTest {
            domainObject(obj)
            exclude(TestTarget.encoders())
            disable<RandomKeysInputTransformer>()
            disable<CorruptionInputTransformer>()
            json = coercingJson
            iterations = 20
            jsonElement {
                "int" eq obj.int
                "inlinedString" eq null
                "inlinedEnum1" eq "Entry1"
                "inlinedEnum2" eq "Entry2"
                "sdd" eq TestEnum.entry2
                "yyy" eq "unknown_entry"
            }
        }
    }

    @Test
    fun coerced2() {
        val obj = Root(
            inlined = Inlined(inlinedEnum2 = null),
            int = 123
        )

        randomizedTest {
            domainObject(obj)
            exclude(TestTarget.encoders())
            disable<RandomKeysInputTransformer>()
            disable<CorruptionInputTransformer>()
            json = coercingJson
            iterations = 20
            jsonElement {
                "int" eq obj.int
                "inlinedString" eq null
                "inlinedEnum1" eq null
                "inlinedEnum2" eq "some_entry"
            }
        }
    }

    @Test
    fun `decodeInlinedMapKeyIndex should validate skipped value`() {
        assertFailsWith<ZeroJsonDecodingException> {
            TestZeroJson.decodeFromString<Root>(
                """{int: 101, inlinedString: zzz, ghi: entry2, inlinedEnum2 :null:}"""
            )
        }
    }
}