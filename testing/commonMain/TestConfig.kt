package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonDecodingException
import dev.dokky.zerojson.framework.transformers.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

data class TestConfig<T>(
    val domainObject: T,
    val jsonElement: JsonElement,
    val serializer: KSerializer<T>,
    val json: ZeroJson = ZeroJson,
    val compareToString: Boolean = false,
    val exclude: Set<TestTarget> = emptySet(),
    val expectTargetFailure: Map<TestTarget, ExpectedFailure> = emptyMap(),
    val expectModeFailure: List<ExpectedFailureForMode> = emptyList(),
    val name: String? = null,
    val iterations: Int = defaultIterationCount(),
    val modesPerIteration: Int = 1 shl 10,
    val transformers: List<TestInputTransformer> = DefaultTransformers,
    val printLine: Boolean = false
) {
    init {
        require(transformers.isNotEmpty()) { "no test transformers" }
        require(iterations > 0) { "invalid 'iterations': $iterations" }
        // downflow components has more sophisticated validation of this
        require(modesPerIteration > 0) { "invalid 'modesPerIteration': $modesPerIteration" }
        require(!TestTarget.entries.all { it in exclude }) { "all targets excluded" }
    }

    class ExpectedFailureForMode(val match: (JsonTestMode) -> Boolean, val failure: ExpectedFailure)

    class ExpectedFailure(
        val exceptionClass: KClass<out Throwable> = SerializationException::class,
        val keepStackTrace: Boolean = false,
        val match: (Throwable) -> Boolean
    ) {
        fun or(other: ExpectedFailure): ExpectedFailure = ExpectedFailure(
            exceptionClass = merge(exceptionClass, other.exceptionClass),
            keepStackTrace = keepStackTrace || other.keepStackTrace,
            match = { err -> this.match(err) || other.match(err) }
        )

        companion object {
            @JvmStatic
            inline operator fun <reified T: Throwable> invoke(
                keepStackTrace: Boolean = false,
                crossinline match: (T) -> Boolean = { true }
            ): ExpectedFailure =
                ExpectedFailure(exceptionClass = T::class, keepStackTrace = keepStackTrace) {
                    it is T && match(it)
                }

            @JvmField
            val AnySerializationException: ExpectedFailure = invoke<SerializationException>()

            private fun merge(e1: KClass<out Throwable>, e2: KClass<out Throwable>): KClass<out Throwable> {
                if (e1 == e2) return e1

                if ((e1 == SerializationException::class && e2 == ZeroJsonDecodingException::class) ||
                    (e2 == SerializationException::class && e1 == ZeroJsonDecodingException::class))
                {
                    return SerializationException::class
                }

                return Throwable::class
            }
        }
    }

    internal companion object {
        val DefaultPrototype: TestConfig<Unit> by lazy { TestConfig(Unit, JsonNull, Unit.serializer()) }

        val DefaultTransformers: List<TestInputTransformer> by lazy {
            listOf<TestInputTransformer>(
                WrapperInputTransformer,
                RandomKeysInputTransformer,
                RandomOrderInputTransformer,
                RandomSpaceInputTransformer(),
                UnquotedStringsInputTransformer,
                TrailingCommaInputTransformer,
                CorruptionInputTransformer
            )
        }

        internal fun defaultIterationCount(): Int = when(GlobalTestMode) {
            TestMode.QUICK -> 100
            TestMode.DEFAULT -> 1_000
            TestMode.FULL -> 10_000
        }
    }
}