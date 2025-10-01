package dev.dokky.zerojson.framework

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

/**
 * Filter over externally defined list of test input transformers.
 *
 * @property bits the most significant one bit corresponds to
 * the first enabled transformer in a list.
 * Example: for transformers of total size 6, the string `010100`
 * encodes a transformers at indices 1 and 3
 */
@JvmInline
@OptIn(InternalTestingApi::class)
value class SelectedTransformers internal constructor(
    @property:InternalTestingApi val bits: Int
) {
    constructor(allTransformers: List<TestInputTransformer>, selectedIndices: Collection<Int>): this(
        selectedIndices.toBits(allTransformers.size)
    )

    val isEmpty: Boolean get() = bits == 0

    val size: Int get() = bits.countOneBits()

    inline fun iterate(transformers: List<TestInputTransformer>, body: (TestInputTransformer) -> Unit) {
        iterateIndices(transformers.size) { index ->
            body(transformers[index])
        }
    }

    inline fun iterateIndices(total: Int, from: Int = 0, body: (Int) -> Unit) {
        var bits = bits
        if (from > 0) {
            bits = bits and (0.inv() shl (total - from)).inv()
        }

        var shift = from
        while (true) {
            shift += (bits shl shift).countLeadingZeroBits() + 1
            if (shift > WORD_SIZE) break
            val bitIndex = WORD_SIZE - (shift - 1)
            body(total - bitIndex)
        }
    }

    fun toList(transformers: List<TestInputTransformer>): List<TestInputTransformer> =
        buildList(size) { iterate(transformers, ::add) }

    fun print(result: TestResult<*>, output: Appendable, verbose: Boolean = false) {
        print(result.config.transformers, output, verbose)
    }

    fun print(transformers: List<TestInputTransformer>, output: Appendable, verbose: Boolean = false) {
        if (isEmpty) return

        var first = true
        iterate(transformers) { transformer ->
            if (first) first = false else output.append(if (verbose) ", " else " ")
            output.append(if (verbose) transformer.name else transformer.code)
        }
    }

    fun toString(transformers: List<TestInputTransformer>, verbose: Boolean = false): String = buildString {
        print(transformers, this, verbose)
    }

    fun isTransformerPresent(transformers: List<TestInputTransformer>, index: Int): Boolean =
        bits and mask(transformers, index) != 0

    fun getTransformerOrNull(transformers: List<TestInputTransformer>, index: Int): TestInputTransformer? =
        if (isTransformerPresent(transformers, index)) transformers[index] else null

    fun withTransformer(transformers: List<TestInputTransformer>, index: Int): SelectedTransformers =
        withTransformer(total = transformers.size, index = index)

    fun withoutTransformer(transformers: List<TestInputTransformer>, index: Int): SelectedTransformers =
        SelectedTransformers(bits and mask(transformers, index).inv())

    private fun withTransformer(total: Int, index: Int): SelectedTransformers =
        SelectedTransformers(bits or mask(total, index))

    fun countCommonSignificantTransformers(other: SelectedTransformers, total: Int): Int {
        val commonPrefixSize = total - (WORD_SIZE - (bits xor other.bits).countLeadingZeroBits())
        return bits.ushr(total - commonPrefixSize).countOneBits()
    }

    fun filter(
        transformers: List<TestInputTransformer>,
        body: (TestInputTransformer) -> Boolean
    ): SelectedTransformers {
        val total = transformers.size
        var result = Empty
        iterateIndices(total) { index ->
            if (body(transformers[index])) result = result.withTransformer(total, index)
        }
        return result
    }

    companion object {
        @PublishedApi internal const val WORD_SIZE: Int = 32

        const val MAX_TRANSFORMERS: Int = WORD_SIZE

        @JvmStatic
        val Empty: SelectedTransformers = SelectedTransformers(0)

        @JvmStatic
        internal fun all(transformers: List<TestInputTransformer>): SelectedTransformers =
            SelectedTransformers((1 shl transformers.size) - 1)

        private fun mask(total: Int, strIndex: Int): Int =
            (1 shl (total - strIndex - 1))

        private fun mask(transformers: List<TestInputTransformer>, index: Int): Int =
            mask(transformers.size, index)

        private fun Collection<Int>.toBits(totalStr: Int): Int {
            require(totalStr <= MAX_TRANSFORMERS) { "Too many transformers: $totalStr. Maximum: $MAX_TRANSFORMERS" }
            var result = 0
            for (idx in this) {
                result = result or mask(totalStr, idx)
            }
            return result
        }
    }
}

fun List<TestInputTransformer>.select(vararg indices: Int): SelectedTransformers = select(indices.toList())

fun List<TestInputTransformer>.select(indices: Collection<Int>): SelectedTransformers = SelectedTransformers(this, indices)
