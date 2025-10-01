package dev.dokky.zerojson.framework

import karamel.utils.forEachBit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.roundToInt
import kotlin.random.Random

@OptIn(InternalTestingApi::class)
internal class TestModeIterator(
    private val allTransformers: List<TestInputTransformer>,
    private val exclude: Set<TestTarget> = emptySet(),
    maxModes: Int = 1 shl 10
) {
    constructor(config: TestConfig<*>): this(config.transformers, config.exclude)

    private val selectedTargets = TestTarget.entries.filterNot { it in exclude }

    init {
        require(maxModes >= selectedTargets.size * 2)
    }

    private val interTransformerIncompatibilityBits = IntArray(allTransformers.size) { i ->
        var result = SelectedTransformers.Empty
        for (incStr in allTransformers[i].incompatibleWith) {
            val conflictIdx = allTransformers.indexOfFirst { it::class == incStr }
            if (conflictIdx >= 0) result = result.withTransformer(allTransformers, conflictIdx)
        }
        result.bits
    }

    private val maxTransformer: Int = 1.shl(allTransformers.size) - 1

    // Example:
    // If ratio is 1.3, then we need to iterate over 1.3 elements in average to not overflow `maxModes`.
    // To iterate 1.3 elements in average, we need a distribution starting from 1.0 with center at 1.3,
    // which is [1, 1.6]
    private val maxJumpDistance: Double = run {
        val maxTransformers = maxModes.toDouble() / selectedTargets.size
        val totalTransformers = 1 shl allTransformers.size
        val ratio = totalTransformers.toDouble() / maxTransformers
        1.0 + (ratio - 1) * 2
    }
    private fun iterationStep(): Double = when {
        maxJumpDistance <= 1 -> 1.0
        else -> Random.nextDouble(1.0, maxJumpDistance)
    }

    private inline fun iterateTemplate(body: (JsonTestMode) -> Unit) {
        var bits = maxTransformer.toDouble()
        var testedTargets = 0

        while (bits >= 0) {
            val set = SelectedTransformers(bits.roundToInt())

            if (!set.hasConflicts()) {
                for ((targetIndex, target) in selectedTargets.withIndex()) {
                    if (target.isCompatibleWith(set)) {
                        testedTargets = testedTargets or (1 shl targetIndex)
                        body(JsonTestMode(target, set))
                    }
                }
            }

            bits -= iterationStep()
        }

        // Check if target is not tested at all.
        // If so, force the test in two modes: no strats and all compatible strats
        forceTestMissingTargets(testedTargets, body)
    }

    private inline fun forceTestMissingTargets(testedTargets: Int, body: (JsonTestMode) -> Unit) {
        val allTargetBits = (1 shl selectedTargets.size) - 1
        val untestedTargets = testedTargets xor allTargetBits
        untestedTargets.forEachBit { targetIndex ->
            val target = selectedTargets[targetIndex]
            val strats = SelectedTransformers(targetIndexToCompatibleTransformerSet[target.ordinal])
            body(JsonTestMode(target, strats))
            body(JsonTestMode(target, SelectedTransformers.Empty))
        }
    }

    @Suppress("LEAKED_IN_PLACE_LAMBDA")
    @OptIn(ExperimentalContracts::class)
    fun forEachMode(body: (JsonTestMode) -> Unit) {
        contract {
            callsInPlace(body)
        }
        iterateTemplate(body)
    }

    fun allModes(): Sequence<JsonTestMode> = sequence {
        iterateTemplate {
            yield(it)
        }
    }

    private fun SelectedTransformers.hasConflicts(): Boolean {
        val bits = bits
        iterateIndices(allTransformers.size) { transformerIndex ->
            if (interTransformerIncompatibilityBits[transformerIndex] and bits != 0) {
                return true
            }
        }
        return false
    }

    private val targetIndexToCompatibleTransformerSet = IntArray(TestTarget.entries.size) { targetIndex ->
        val target = TestTarget.entries[targetIndex]

        var result = SelectedTransformers.Empty
        if (target !in exclude) {
            for ((strIdx, transformer) in allTransformers.withIndex()) {
                result = when (target) {
                    in transformer.targets -> result.withTransformer(allTransformers, strIdx)
                    else -> result.withoutTransformer(allTransformers, strIdx)
                }
            }
        }
        result.bits
    }

    private fun TestTarget.isCompatibleWith(transformers: SelectedTransformers): Boolean {
        return transformers.bits and targetIndexToCompatibleTransformerSet[ordinal] == transformers.bits
    }

    fun isValid(mode: JsonTestMode): Boolean {
        return mode.target.isCompatibleWith(mode.transformers) && !mode.transformers.hasConflicts()
    }

    fun approximateMaximumIterations(): Int = maxTransformer
}