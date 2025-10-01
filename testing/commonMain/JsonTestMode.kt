package dev.dokky.zerojson.framework

import io.kodec.struct.BitStruct
import io.kodec.struct.composeIntSafe
import io.kodec.struct.get
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

@JvmInline
value class JsonTestMode private constructor(private val asInt: Int) {
    @OptIn(InternalTestingApi::class)
    constructor(target: TestTarget, transformers: SelectedTransformers): this(
        composeIntSafe {
            JsonTestModeSchema.target set target.ordinal
            JsonTestModeSchema.transformerBits set transformers.bits
        }
    )

    val target: TestTarget get() = TestTarget.entries[asInt.get(JsonTestModeSchema.target)]
    val transformers: SelectedTransformers get() = SelectedTransformers(asInt.get(JsonTestModeSchema.transformerBits))

    fun toInt(): Int = asInt

    fun print(
        result: TestResult<*>,
        output: Appendable,
        verbose: Boolean = false
    ) {
        print(result.config.transformers, output, verbose)
    }

    fun print(
        allTransformers: List<TestInputTransformer>,
        output: Appendable,
        verbose: Boolean = false
    ) {
        output.append(target.toString())

        if (!transformers.isEmpty) {
            output.append('(')
            transformers.print(allTransformers, output, verbose = verbose)
            output.append(')')
        }
    }

    fun toPrettyString(allTransformers: List<TestInputTransformer>, verbose: Boolean = false): String =
        buildString { print(allTransformers, this, verbose) }

    fun toPrettyString(result: TestResult<*>, verbose: Boolean = false): String =
        toPrettyString(result.config.transformers, verbose)

    override fun toString(): String = buildString {
        append(target)
        append('(')
        append(asInt.get(JsonTestModeSchema.transformerBits))
        if (last() == '(') setLength(length - 1) else append(')')
    }
}

private object JsonTestModeSchema: BitStruct(32) {
    @JvmStatic val target = int(5)
    @JvmStatic val transformerBits = int(32 - target.size)
}