package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.TestTarget.DataType
import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomOrderInputTransformer
import dev.dokky.zerojson.framework.transformers.WrapperInputTransformer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleRunnerTests: AbstractRunnerTest() {
    @Test
    fun all_ok() {
        doRandomizedTest(config(injectEncoderError = false, injectDecoderError = false))
            .assertAllModesOk()
    }

    @Test
    fun serializer_should_be_called() {
        val serializer = BuggySerializer(encoderFailAt = -1, decoderFailAt = -1)

        val cfg = TestConfig {
            domainObject(simpleData, serializer)
            jsonElement = expectedJsonElement
            this.iterations = 10
            configureTestCorruption()
        }

        randomizedTest(cfg)

        val wrTransformerIndex = cfg.transformers.indexOf(WrapperInputTransformer)

        var serialsPerIteration = 0
        TestModeIterator(cfg).forEachMode { mode ->
            if (mode.target.input == DataType.Domain) {
                serialsPerIteration += when {
                    mode.transformers.isTransformerPresent(cfg.transformers, wrTransformerIndex) -> 2
                    else -> 1
                }
            }
        }

        // A simple check to make sure TestModeIterator is probably alright.
        // Of course, there is separate test for TMI, but it's better to fail here too.
        assertTrue(serialsPerIteration >= 4)
        assertEquals(serialsPerIteration * cfg.iterations, serializer.serializeCalled)
    }

    @Test
    fun deserializer_should_be_called() {
        val serializer = BuggySerializer(encoderFailAt = -1, decoderFailAt = -1)

        val cfg = TestConfig {
            domainObject(simpleData, serializer)
            jsonElement = expectedJsonElement
            this.iterations = 1
            // (WR COR) modes may call deserializer 1 OR 2 times (randomly)
            disable<CorruptionInputTransformer>()
            disable<RandomOrderInputTransformer>()
        }

        randomizedTest(cfg)

        val wrTransformerIndex = cfg.transformers.indexOf(WrapperInputTransformer)

        var deserialisationsPerIteration = 0
        TestModeIterator(cfg).forEachMode { mode ->
            if (mode.target.output == DataType.Domain) {
                deserialisationsPerIteration += when {
                    mode.transformers.isTransformerPresent(cfg.transformers, wrTransformerIndex) -> 2
                    else -> 1
                }
            }
        }

        // A simple check to make sure TestModeIterator is probably alright.
        // Of course, there is separate test for TMI, but it's better to fail here too.
        assertTrue(deserialisationsPerIteration > 50)
        assertEquals(deserialisationsPerIteration * cfg.iterations, serializer.deserializeCalled)
    }

    @Test
    fun include_only() {
        val result = doRandomizedTest {
            configure(injectEncoderError = false)
            includeOnly(TestTarget.ObjectToText)
        }

        val (success, excluded) = result.config.allModes().partition { it.target == TestTarget.ObjectToText }
        assertSetEquals(excluded, result.excludedModes())
        assertSetEquals(success, result.successModes())
    }

    @Test
    fun n_success_iterations() {
        val result = doRandomizedTest {
            domainObject(simpleData, BuggySerializer(decoderFailAt = 19))
            jsonElement = expectedJsonElement
            includeOnly(TestTarget.TreeToObject)
            iterations = 10
            disable<RandomOrderInputTransformer>()
        }

        result.assertAllFailed()
    }
}