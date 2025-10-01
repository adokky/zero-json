package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomOrderInputTransformer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.assertTrue

abstract class AbstractRunnerTest: RandomizedJsonTest() {
    protected val simpleData = SimpleDataClass("zzz")
    protected val expectedJsonElement = Json.encodeToJsonElement(simpleData).jsonObject

    protected fun TestConfigBuilder.configureTestCorruption() {
        val corrStrIndex = transformers.indexOf(CorruptionInputTransformer)
        expectFailureIfMode(
            TestConfig.ExpectedFailure<Throwable> { it !is TestException },
            match = { it.transformers.isTransformerPresent(transformers, corrStrIndex) }
        )
    }

    protected fun config(
        injectEncoderError: Boolean = true,
        injectDecoderError: Boolean = true,
        randomKeys: Boolean = true,
        corruption: Boolean = true
    ) = TestConfig {
        configure(
            injectEncoderError = injectEncoderError,
            injectDecoderError = injectDecoderError,
            randomKeys = randomKeys,
            corruption = corruption
        )
    }

    protected fun TestConfigBuilder.configure(
        injectEncoderError: Boolean = true,
        injectDecoderError: Boolean = true,
        randomKeys: Boolean = true,
        corruption: Boolean = true
    ) {
        domainObject(simpleData,
            BuggySerializer(
                encoderFailAt = if (injectEncoderError) 0 else -1,
                decoderFailAt = if (injectDecoderError) 0 else -1,
            )
        )

        jsonElement = expectedJsonElement
        iterations = 3

        disable<RandomOrderInputTransformer>() // by its nature can be randomly skipped
        if (!randomKeys) disable<RandomKeysInputTransformer>()
        if (corruption) configureTestCorruption() else disable<CorruptionInputTransformer>()
    }

    protected fun TestResult<*>.assertAllModesOk(): TestResult<*> {
        assertTrue(results.isNotEmpty())
        val nonSuccesses = modesToString { it !is JsonTestResult.Ok }
        assertTrue(
            nonSuccesses.isEmpty(),
            "expected all modes to be OK, but some not:\n$nonSuccesses"
        )
        assertSetEquals(
            config.allModes().toList(),
            results.map { it.mode }
        )
        return this
    }

    protected fun TestResult<*>.assertNoSkipped(): TestResult<*> {
        assertTrue(results.isNotEmpty())
        assertTrue(
            results.all { it !is JsonTestResult.Skipped },
            "expected no Skipped modes, but some present:\n${skippedModes().joinToString("\n")}"
        )
        return this
    }

    protected fun TestResult<*>.assertAllFailed(): TestResult<*> {
        assertTrue(results.isNotEmpty())
        assertSetEquals(config.allModes().toList(), failedModes()) {
            it.print(this@assertAllFailed, this, verbose = false)
        }
        return this
    }

    protected fun TestConfig<*>.allModes(): Sequence<JsonTestMode> =
        TestModeIterator(this).allModes()

    protected fun TestResult<*>.modesToString(
        separator: String = "\n",
        filter: (JsonTestResult) -> Boolean
    ): String {
        return buildString {
            var first = true
            for (res in results) {
                if (filter(res)) {
                    if (!first) append(separator) else first = false
                    append("Mode: ")
                    res.mode.print(this@modesToString, this)
                    appendLine()
                    res.print(this)
                }
            }
        }
    }

    protected fun TestResult<*>.assertModesEquals(
        expected: Collection<JsonTestMode>,
        actual: Collection<JsonTestMode>
    ) {
        assertSetEquals(expected, actual) {
            it.print(result = this@assertModesEquals, output = this)
        }
    }
}