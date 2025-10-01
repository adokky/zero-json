package dev.dokky.zerojson.framework

import karamel.utils.NoStackTraceThrowable
import kotlinx.serialization.json.JsonNull
import kotlin.test.*

class TestResultBuilderTest {
    private val config = TestConfig {
        domainObject(Unit)
        jsonElement = JsonNull
        expectFailure(TestTarget.ObjectToBinary)
        exclude(TestTarget.TextToTree)
        iterations = 10
    }

    private val modeIterator = TestModeIterator(config)
    private val resultBuilder = TestResultBuilderImpl(modeIterator, maxIterations = config.iterations, assertions = true)

    @Test
    fun state_ok() {
        val state = ModeState.ok(5)
        assertFalse(state.isExcluded)
        assertTrue(state.isOk)
        assertFalse(state.isFailed)
        assertFalse(state.isCompleted)
        assertEquals(-1, state.failedAtIteration)
        assertEquals(5, state.successIterations)
    }

    @Test
    fun state_failure() {
        val state = ModeState.failure(5)
        assertFalse(state.isExcluded)
        assertFalse(state.isOk)
        assertTrue(state.isFailed)
        assertTrue(state.isCompleted)
        assertEquals(5, state.failedAtIteration)
        assertEquals(0, state.successIterations)
    }

    @Test
    fun state_excluded() {
        assertTrue(ModeState.excluded.isExcluded)
        assertFalse(ModeState.excluded.isOk)
        assertFalse(ModeState.excluded.isFailed)
        assertTrue(ModeState.excluded.isCompleted)
        assertEquals(-1, ModeState.excluded.failedAtIteration)
        assertEquals(0, ModeState.excluded.successIterations)
    }

    @Test
    fun mode_exclusion() {
        resultBuilder.clear()
        val mode = JsonTestMode(TestTarget.TextToTree, config.transformers.select(1, 5))
        resultBuilder.exclude(mode)
        assertEquals(ModeState.excluded, resultBuilder.stateOf(mode))
        assertEquals(ModeState.ok(0), resultBuilder.stateOf(JsonTestMode(TestTarget.TextToTree, config.transformers.select(1))))

        assertIs<JsonTestResult.Excluded>(assertAllSkippedExcept(mode))
    }

    @Test
    fun mode_success() {
        resultBuilder.clear()
        val mode = JsonTestMode(TestTarget.BinaryToTree, config.transformers.select(1, 4))

        resultBuilder.success(mode)
        var state = resultBuilder.stateOf(mode)
        assertTrue(state.isOk)
        assertFalse(state.isExcluded)
        assertFalse(state.isFailed)
        assertEquals(1, state.successIterations)

        resultBuilder.success(mode)
        state = resultBuilder.stateOf(mode)
        assertEquals(2, state.successIterations)
        assertTrue(state.isOk)
        assertFalse(state.isExcluded)
        assertFalse(state.isFailed)

        val res = assertAllSkippedExcept(mode)
        assertIs<JsonTestResult.Ok>(res)
        assertEquals(config.iterations - 2, res.skipped)
    }

    @Test
    fun mode_failure() {
        resultBuilder.clear()
        val mode = JsonTestMode(TestTarget.TextToTree, config.transformers.select(1, 5))

        val failure = JsonTestResult.Failure.Simple(mode, 7, null, NoStackTraceThrowable())
        resultBuilder.fail(failure)

        val state = resultBuilder.stateOf(mode)
        assertFalse(state.isOk)
        assertFalse(state.isExcluded)
        assertTrue(state.isFailed)
        assertEquals(7, state.failedAtIteration)

        val res = assertAllSkippedExcept(mode)
        assertEquals(failure, res)
    }

    private fun assertAllSkippedExcept(testMode: JsonTestMode): JsonTestResult {
        val result = resultBuilder.complete()
        var found: JsonTestResult? = null
        modeIterator.forEachMode { mode ->
            val res = result.single { it.mode == mode }
            if (mode == testMode) {
                found = res
            } else {
                assertIs<JsonTestResult.Skipped>(res)
            }
        }
        if (found == null) found = result.singleOrNull { it.mode == testMode }
        assertNotNull(found, "no JsonTestResult for $testMode")
        return found
    }
}