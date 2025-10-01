package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import dev.dokky.zerojson.framework.DslJsonObjectBuilder
import dev.dokky.zerojson.framework.jsonObject
import kotlinx.serialization.Serializable
import kotlin.test.Test

class CoercingTest: AbstractDecoderTest(ZeroJsonConfiguration(
    coerceInputValues = true,
    structuredMapKeysMode = StructuredMapKeysMode.LIST
)) {
    @Test
    fun coercing_inside_composite_key() {
        assertDecodedEquals(
            """[{"value":"unknown"}, OptionC, {"value":"entry2"}, OptionA]""",
            mapOf<Box<TestEnum?>, SampleEnum>(
                Box<TestEnum?>(null) to SampleEnum.OptionC,
                Box<TestEnum?>(TestEnum.entry2) to SampleEnum.OptionA,
            ),
            testTreeDecoder = true
        )
    }

    @Test
    fun nullable_enum() {
        assertDecodedEquals<Box<TestEnum?>>(
            """{"value":"unknown"}""",
            Box(null),
            testTreeDecoder = true
        )
    }

    @Serializable
    private data class OptionalEnum(
        val enum1: TestEnum? = TestEnum.entry2,
        val enum2: TestEnum = TestEnum.`entry 3`,
        val enum3: TestEnum = TestEnum.entry1,
    )

    @Test
    fun optional_enum() {
        assertDecodedEquals<OptionalEnum>(
            """{"enum1":"unknown", "enum2":"unknown", "enum3":null}""",
            OptionalEnum(),
            testTreeDecoder = true
        )
    }

    @Test
    fun coercing_off() {
        fun test(input: DslJsonObjectBuilder.() -> Unit) {
            assertDecodingFails(OptionalEnum.serializer(), jsonObject(buildJson = input), message = "unknown entry", json = ZeroJson)
        }

        test { "enum1" eq "zzz" }
        test { "enum2" eq "zzz" }
        test { "enum3" eq "zzz" }
    }
}