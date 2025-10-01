package dev.dokky.zerojson.framework

import karamel.utils.unsafeCast
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlin.test.*

class TestCaseRunnerTest {
    private val config = TestConfig {
        domainObject(Unit)
        jsonElement = JsonNull
        expectFailure(TestTarget.ObjectToBinary)
        exclude(TestTarget.TextToTree)
        iterations = 10
    }

    private val modeIterator = TestModeIterator(config)
    private val resultBuilder = TestResultBuilderImpl(modeIterator, maxIterations = config.iterations, assertions = true)
    private val testInput = testInputImpl(config)

    private fun testInputImpl(config: TestConfig<*>): TestInputImpl = TestInputImpl(
        json = config.json,
        domainObject = config.domainObject,
        serializer = config.serializer.unsafeCast(),
        jsonElement = config.jsonElement,
        composerConfig = BaseJsonComposerConfig(),
        composer = DefaultJsonComposer(StringBuilder())
    )

    private fun completeAndGetResultFor(mode: JsonTestMode): JsonTestResult = 
        resultBuilder.complete().single { it.mode == mode }

    @Test
    fun ok() {
        val runner = TestCaseRunner(config, resultBuilder)

        val mode = JsonTestMode(TestTarget.TextToObject, SelectedTransformers.Empty)
        val success = runner.run(mode, testInput, inputObject = Unit, iteration = 1) {}
        assertTrue(success)

        assertEquals(ModeState.ok(1), resultBuilder.stateOf(mode))

        val result = completeAndGetResultFor(mode)
        assertEquals(mode, result.mode)
        assertIs<JsonTestResult.Ok>(result)
        assertEquals(config.iterations - 1, result.skipped)
    }

    @Test
    fun excluded() {
        val runner = TestCaseRunner(config, resultBuilder)

        val mode = JsonTestMode(TestTarget.TextToTree, SelectedTransformers.Empty)
        val success = runner.run(mode, testInput, inputObject = Unit, iteration = 1) { fail() }
        assertFalse(success)

        assertEquals(ModeState.excluded, resultBuilder.stateOf(mode))

        val result = completeAndGetResultFor(mode)
        assertEquals(mode, result.mode)
        assertIs<JsonTestResult.Excluded>(result)
    }

    @Test
    fun simple_failure() {
        val runner = TestCaseRunner(config, resultBuilder)

        val mode = JsonTestMode(TestTarget.BinaryToTree, SelectedTransformers.Empty)
        val success = runner.run(mode, testInput, inputObject = Unit, iteration = 1) {
            throw RuntimeException("test")
        }
        assertFalse(success)
        
        assertEquals(ModeState.failure(1), resultBuilder.stateOf(mode))
        
        val failure = completeAndGetResultFor(mode)
        assertEquals(mode, failure.mode)
        assertIs<JsonTestResult.Failure.Simple>(failure)
        assertEquals(1, failure.iteration)
        assertEquals(Unit, failure.input)
        assertIs<RuntimeException>(failure.exception)
        assertEquals("test", failure.exception.message)
    }

    @Test
    fun invalid_exception() {
        val runner = TestCaseRunner(config, resultBuilder)

        val mode = JsonTestMode(TestTarget.ObjectToBinary, SelectedTransformers.Empty)
        val success = runner.run(mode, testInput, inputObject = Unit, iteration = 1) {
            throw RuntimeException("test2")
        }
        assertFalse(success)

        assertEquals(ModeState.failure(1), resultBuilder.stateOf(mode))

        val failure = completeAndGetResultFor(mode)
        assertEquals(mode, failure.mode)
        assertIs<JsonTestResult.Failure.InvalidException>(failure)
        assertEquals(1, failure.iteration)
        assertEquals(Unit, failure.input)
        assertEquals(SerializationException::class, failure.expected)
        assertIs<RuntimeException>(failure.actual)
        assertEquals("test2", failure.actual.message)
    }

    @Test
    fun missing_exception() {
        val runner = TestCaseRunner(config, resultBuilder)

        val mode = JsonTestMode(TestTarget.ObjectToBinary, SelectedTransformers.Empty)
        val success = runner.run(mode, testInput, inputObject = Unit, iteration = 1) {}
        assertFalse(success)

        assertEquals(ModeState.failure(1), resultBuilder.stateOf(mode))

        val failure = completeAndGetResultFor(mode)
        assertEquals(mode, failure.mode)
        assertIs<JsonTestResult.Failure.MissingException>(failure)
        assertEquals(1, failure.iteration)
        assertEquals(Unit, failure.input)
        assertEquals(SerializationException::class, failure.expected)
    }

    private class TestException(m: String): RuntimeException(m)

    @Test
    fun many_expected_failures() {
        val testTarget = TestTarget.TextToObject
        val testMode1 = JsonTestMode(testTarget, SelectedTransformers.Empty)
        val testMode2 = JsonTestMode(testTarget, SelectedTransformers(TestConfig.DefaultTransformers, listOf(1, 4)))
        val testMode3 = JsonTestMode(testTarget, SelectedTransformers(TestConfig.DefaultTransformers, listOf(1)))
        val testMode4 = JsonTestMode(testTarget, SelectedTransformers(TestConfig.DefaultTransformers, listOf(4)))

        val config = TestConfig {
            domainObject(Unit)
            jsonElement = JsonNull
            iterations = 10

            expectFailure(testTarget)
            expectFailure<TestException>(testTarget)
            expectFailure<TestException>(listOf(testMode1))
            expectFailure<RuntimeException>(listOf(testMode2), exactMessage = "123")
            expectFailure<RuntimeException>(listOf(testMode3), exactMessage = "456")
        }

        val modeIterator = TestModeIterator(config)
        val resultBuilder = TestResultBuilderImpl(modeIterator, maxIterations = config.iterations, assertions = true)
        val testInput = testInputImpl(config)
        val runner = TestCaseRunner(config, resultBuilder)

        assertFalse(runner.run(testMode1, testInput, inputObject = Unit, iteration = 1) { throw SerializationException() })
        assertFalse(runner.run(testMode2, testInput, inputObject = Unit, iteration = 1) { throw TestException("123x") })
        assertTrue (runner.run(testMode3, testInput, inputObject = Unit, iteration = 1) { throw TestException("456") })
        assertTrue (runner.run(testMode4, testInput, inputObject = Unit, iteration = 1) { throw TestException("test 4") })

        assertFalse(runner.run(testMode1, testInput, inputObject = Unit, iteration = 2) { fail() })
        assertFalse(runner.run(testMode2, testInput, inputObject = Unit, iteration = 2) { fail() })
        assertFalse(runner.run(testMode4, testInput, inputObject = Unit, iteration = 2) { fail() })

        val result = resultBuilder.complete()
        val r1 = result.single { it.mode == testMode1 }
        val r2 = result.single { it.mode == testMode2 }
        val r3 = result.single { it.mode == testMode3 }
        val r4 = result.single { it.mode == testMode4 }

        assertIs<JsonTestResult.Failure.InvalidException>(r1)
        assertEquals(1, r1.iteration)
        assertEquals(TestException::class, r1.expected)
        assertIs<SerializationException>(r1.actual)

        assertIs<JsonTestResult.Failure.InvalidException>(r2)
        assertEquals(1, r2.iteration)
        assertEquals(RuntimeException::class, r2.expected)
        assertIs<RuntimeException>(r2.actual)

        assertIs<JsonTestResult.Ok>(r3, r3.toString())
        assertEquals(9, r3.skipped)

        assertIs<JsonTestResult.Failure.InvalidException>(r4, r4.toString())
        assertEquals(2, r4.iteration)
        assertEquals(TestException::class, r4.expected)
        assertIs<AssertionError>(r4.actual)
    }
}