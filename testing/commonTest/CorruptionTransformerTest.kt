package dev.dokky.zerojson.framework

import dev.dokky.zerojson.TestZeroJson
import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.WrapperInputTransformer
import karamel.utils.unsafeCast
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.fail

class CorruptionTransformerTest: AbstractRunnerTest() {
    @Test
    fun no_failure() {
        randomizedTest {
            domainObject(SimpleDataClass("string"))
            jsonElement { "key" eq "string" }
            transformers = mutableListOf(CorruptionInputTransformer)
            iterations = 3
        }
    }

    @Test
    fun no_failure_with_custom_EF() {
        randomizedTest {
            domainObject(SimpleDataClass("string"))
            jsonElement { "key" eq "string" }
            transformers = mutableListOf(CorruptionInputTransformer, WrapperInputTransformer)
            iterations = 3
            expectFailureIfMode(TestConfig.ExpectedFailure.AnySerializationException) { false }
        }
    }

    @Test
    fun corruption_result_should_not_have_unquoted_strings() {
        val data = jsonObject { "key" eq "value" }
        val sb = StringBuilder()
        val composer = DefaultJsonComposer(sb)
        val input = TestInputImpl(TestZeroJson, Unit, Unit.serializer().unsafeCast(), JsonNull, composer.config, composer)

        repeat(
            when(GlobalTestMode) {
                TestMode.QUICK -> 100_000
                TestMode.DEFAULT -> 1_000_000
                TestMode.FULL -> 4_000_000
            }
        ) {
            sb.clear()
            input.clear()
            input.composerConfig = composer.config
            input.composer = composer

            CorruptionInputTransformer.transform(input)
            composer.config = input.composerConfig
            composer.appendElement(data)
            val transformText = input.textTransformer ?: fail()
            transformText(sb)

            if (sb.count { it == '"' } % 2 != 0) fail(sb.toString().removeLineBreaks())
        }
    }
}