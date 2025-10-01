package dev.dokky.zerojson.framework

import kotlinx.serialization.SerializationException
import kotlin.jvm.JvmName

@JvmName("expectTargetFailure")
inline fun <reified T: Throwable> TestConfigBuilder.expectFailure(
    targets: Iterable<TestTarget>,
    exactMessage: String? = null,
    containsMessageSubString: String? = null
) {
    expectFailure(
        failure = expectedFailure<T>(
            exactMessage = exactMessage,
            containsMessageSubString = containsMessageSubString
        ),
        targets = targets
    )
}

@JvmName("expectModeFailure")
inline fun <reified T: Throwable> TestConfigBuilder.expectFailure(
    modes: Iterable<JsonTestMode>,
    exactMessage: String? = null,
    containsMessageSubString: String? = null
) {
    expectFailure(modes, expectedFailure<T>(
        exactMessage = exactMessage,
        containsMessageSubString = containsMessageSubString
    ))
}

@JvmName("expectModeFailure")
inline fun <reified T: Throwable> TestConfigBuilder.expectFailureIfMode(
    exactMessage: String? = null,
    containsMessageSubString: String? = null,
    noinline match: (JsonTestMode) -> Boolean
) {
    expectFailureIfMode(expectedFailure<T>(
        exactMessage = exactMessage,
        containsMessageSubString = containsMessageSubString
    ), match)
}

@JvmName("expectModeFailure")
fun TestConfigBuilder.expectFailure(
    modes: Iterable<JsonTestMode>,
    failure: TestConfig.ExpectedFailure
) {
    val modes = modes.toSet()
    expectFailureIfMode(
        failure = failure,
        match = { it in modes }
    )
}

inline fun <reified T: Throwable> TestConfigBuilder.expectFailure(
    target0: TestTarget,
    vararg targets: TestTarget
) {
    expectFailure<T>(targets.toList() + target0)
}

@JvmName("expectModesFailureIf")
inline fun <reified T: Throwable> TestConfigBuilder.expectFailureIf(
    modes: Iterable<JsonTestMode>,
    keepStackTrace: Boolean = false,
    noinline exceptionMatch: (T) -> Boolean
) {
    expectFailure(
        modes = modes,
        failure = TestConfig.ExpectedFailure<T>(keepStackTrace = keepStackTrace, exceptionMatch)
    )
}

@JvmName("expectTargetsFailureIf")
inline fun <reified T: Throwable> TestConfigBuilder.expectFailureIf(
    targets: Iterable<TestTarget>,
    crossinline exceptionMatch: (T) -> Boolean
) {
    expectFailure(
        TestConfig.ExpectedFailure(T::class) { exceptionMatch(it as T) },
        targets.toList()
    )
}

inline fun <reified T: Throwable> TestConfigBuilder.expectFailureIf(
    target0: TestTarget,
    vararg targets: TestTarget,
    crossinline exceptionMatch: (T) -> Boolean
) {
    expectFailureIf(targets.toList() + target0, exceptionMatch)
}

@JvmName("expectTargetSerializationException")
fun TestConfigBuilder.expectFailure(targets: Iterable<TestTarget>) {
    expectFailure<SerializationException>(targets)
}

// version to save some inlining budget
fun TestConfigBuilder.expectSerializationException(targets: Iterable<JsonTestMode>) {
    expectFailure<SerializationException>(targets)
}

@JvmName("expectSerializationException")
fun TestConfigBuilder.expectFailure(target0: TestTarget, vararg targets: TestTarget) {
    expectFailure<SerializationException>(target0, *targets)
}

inline fun <reified T: Throwable> TestConfigBuilder.expectFailureIfTarget(
    targetFilter: (TestTarget) -> Boolean
) {
    expectFailure<T>(TestTarget.entries.filter(targetFilter))
}

@JvmName("expectSerializationExceptionIfTarget")
inline fun TestConfigBuilder.expectFailureIfTarget(targetFilter: (TestTarget) -> Boolean) {
    expectFailureIfTarget<SerializationException>(targetFilter)
}

fun TestConfigBuilder.expectSerializationExceptionIfMode(
    match: (JsonTestMode) -> Boolean
) {
    expectFailureIfMode<SerializationException>(match = match)
}

@PublishedApi
internal inline fun <reified T : Throwable> expectedFailure(
    exactMessage: String?,
    containsMessageSubString: String?
): TestConfig.ExpectedFailure = TestConfig.ExpectedFailure(
    exceptionClass = T::class
) { exception ->
    if (exception !is T) return@ExpectedFailure false

    if (exactMessage != null && exception.message != exactMessage)
        return@ExpectedFailure false

    if (containsMessageSubString != null &&
        exception.message?.contains(containsMessageSubString) != true)
        return@ExpectedFailure false

    true
}