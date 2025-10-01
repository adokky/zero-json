package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.TestTarget.DataType
import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomOrderInputTransformer
import dev.dokky.zerojson.framework.transformers.UnquotedStringsInputTransformer
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FailureCases: AbstractRunnerTest() {
    @Test
    fun buggy_transformer() {
        var busTrIndex = -1

        val result = doRandomizedTest {
            domainObject(SimpleDataClass("string"))
            jsonElement { "key" eq "string" }
            disable<RandomOrderInputTransformer>()
            disable<CorruptionInputTransformer>()
            busTrIndex = transformers.indexOf(UnquotedStringsInputTransformer)
            transformers[busTrIndex] = object : TestInputTransformer(
                "Buggy unquoted strings",
                targets = TestTarget.entries.filter { it.input == DataType.Text })
            {
                override fun transform(input: MutableTestInput) {
                    input.json = input.json.lenient(false)
                    input.composerConfig = input.composerConfig.copy(unquoted = true)
                }
            }
        }

        val allModes = result.config.allModes().toList()
        assertTrue(allModes.size > 50)

        val (expectedFailure, expectedSuccess) = allModes
            .partition { it.transformers.isTransformerPresent(result.config.transformers, busTrIndex) }

        result.assertModesEquals(expectedFailure, result.failedModes())
        result.assertModesEquals(expectedSuccess, result.successModes())
    }

    @Test
    fun mismatch_test() {
        val result = doRandomizedTest {
            domainObject(SimpleDataClass("RED"))
            jsonElement { "key" eq "GREEN" }
            disable<RandomOrderInputTransformer>()
            disable<CorruptionInputTransformer>()
        }

        result.assertModesEquals(result.config.allModes().filter { it.target.isDomain() }.toList(), result.failedModes())
        result.assertModesEquals(result.config.allModes().filter { !it.target.isDomain() }.toList(), result.successModes())
        result.assertNoSkipped()
    }

    @Test
    fun should_fail() {
        // prettifyException=true uses prettyFail which just prints major part of exception message
        assertFailsWith<AssertionError> {
            randomizedTest(config(), prettifyException = false, shortMessage = false)
        }
        assertFailsWith<AssertionError> {
            randomizedTest(config(), prettifyException = false, shortMessage = true)
        }
    }

    @Test
    fun all_mode_failed() {
        val result = doRandomizedTest {
            configure(corruption = false)
            includeTargetIf { it.isDomain() }
        }
        result.assertNoSkipped()
        assertSetEquals(
            result.config.allModes().filter { it.target.isDomain() }.toList(),
            result.failedModes()
        )
    }

    @Test
    fun all_random_keys_modes_should_fail() {
        val result = doRandomizedTest {
            disable<RandomOrderInputTransformer>()
            domainObject(mapOf("aaa" to "zzz"))
            jsonElement { "aaa" eq "zzz" }
            includeTargetIf { it.output == DataType.Domain }
        }

        val rkStrIndex = result.config.transformers.indexOf(RandomKeysInputTransformer)

        assertSetEquals(
            result.config.allModes()
                .filter { it.target.output == DataType.Domain }
                .filter { it.transformers.isTransformerPresent(result.config.transformers, rkStrIndex) }
                .toList(),
            result.results
                .filterIsInstance<JsonTestResult.Failure.Simple>()
                .map { it.mode }
                .filter { it.transformers.isTransformerPresent(result.config.transformers, rkStrIndex) }
                .toList()
        )
    }
}