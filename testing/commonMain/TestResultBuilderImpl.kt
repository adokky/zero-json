@file:OptIn(InternalTestingApi::class)

package dev.dokky.zerojson.framework

import androidx.collection.MutableIntIntMap
import kotlin.jvm.JvmInline

internal interface TestResultBuilder {
    fun stateOf(mode: JsonTestMode): ModeState
    fun isCompleted(mode: JsonTestMode): Boolean
    fun isCompletedSuccessfully(mode: JsonTestMode): Boolean

    fun fail(result: JsonTestResult.Failure)
    fun exclude(mode: JsonTestMode)
    fun success(mode: JsonTestMode, previous: ModeState = stateOf(mode))

    fun complete(): List<JsonTestResult>

    fun clear()
}

internal class TestResultBuilderImpl(
    private val modeIterator: TestModeIterator,
    val maxIterations: Int,
    private val assertions: Boolean = false
): TestResultBuilder {
    private var results = allocateResults()
    private val completedModes = MutableIntIntMap()

    private fun complete(result: JsonTestResult, state: ModeState) {
        setState(result.mode, state)
        results.add(result)
    }

    override fun fail(result: JsonTestResult.Failure) {
        ensureNotCompleted(result.mode)
        complete(result, ModeState.failure(result.iteration))
    }

    override fun exclude(mode: JsonTestMode) {
        ensureNotCompleted(mode)
        complete(JsonTestResult.Excluded(mode), ModeState.excluded)
    }

    override fun success(mode: JsonTestMode, previous: ModeState) {
        if (assertions) {
            val actualPrev = stateOf(mode)
            require(!actualPrev.isCompleted) { "mode $mode is already completed" }
            require(actualPrev.successIterations == previous.successIterations) {
                "invalid 'previous': ${previous.successIterations}. Actual: $actualPrev"
            }
        }

        completedModes.put(mode.toInt(), previous.successIterations + 1)
    }

    private fun ensureNotCompleted(mode: JsonTestMode) {
        if (!assertions) return
        require(!isCompleted(mode)) { "mode $mode is already completed" }
    }

    override fun stateOf(mode: JsonTestMode): ModeState = ModeState.fromInt(completedIterations(mode))

    private fun setState(mode: JsonTestMode, state: ModeState) {
        completedModes.put(mode.toInt(), state.asInt)
    }

    private fun completedIterations(mode: JsonTestMode): Int = completedModes.getOrDefault(mode.toInt(), 0)

    override fun isCompleted(mode: JsonTestMode): Boolean = completedIterations(mode) !in 0..<maxIterations

    override fun isCompletedSuccessfully(mode: JsonTestMode): Boolean = completedIterations(mode) >= maxIterations

    override fun complete(): List<JsonTestResult> {
        modeIterator.forEachMode { mode ->
            val completed = completedIterations(mode)
            when {
                completed == 0 -> results.add(JsonTestResult.Skipped(mode))
                completed > 0 -> results.add(JsonTestResult.Ok(mode, skipped = maxIterations - completed))
            }
        }

        val result = results
        clear()
        return result
    }

    override fun clear() {
        results = allocateResults()
        completedModes.clear()
    }

    private fun allocateResults(): ArrayList<JsonTestResult> =
        ArrayList(modeIterator.approximateMaximumIterations())
}

@JvmInline
internal value class ModeState private constructor(internal val asInt: Int) {
    val isFailed: Boolean get() = asInt > Int.MIN_VALUE && asInt < 0
    val isExcluded: Boolean get() = asInt == Int.MIN_VALUE
    val isOk: Boolean get() = asInt >= 0
    val isCompleted: Boolean get() = asInt < 0

    val failedAtIteration: Int get() = if (isFailed) -asInt else -1
    val successIterations: Int get() = if (asInt > 0) asInt else 0

    override fun toString(): String = when {
        isFailed -> "Failed(iteration: $failedAtIteration)"
        isExcluded -> "Excluded"
        isOk -> "Success(iterations: $successIterations)"
        else -> "Ok(iterations: $successIterations)"
    }

    companion object {
        @InternalTestingApi fun fromInt(asInt: Int): ModeState = ModeState(asInt)

        fun failure(iteration: Int): ModeState {
            require(iteration > 0) { "invalid iteration: $iteration" }
            return ModeState(-iteration)
        }

        fun ok(iterations: Int): ModeState {
            require(iterations >= 0) { "invalid iterations: $iterations" }
            return ModeState(iterations)
        }

        val excluded: ModeState = ModeState(Int.MIN_VALUE)
    }
}