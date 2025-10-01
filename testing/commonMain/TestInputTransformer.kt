package dev.dokky.zerojson.framework

import karamel.utils.splitIterate
import kotlin.reflect.KClass

// todo alwaysEnabled option
abstract class TestInputTransformer(
    val name: String,
    val targets: List<TestTarget>,
    val code: String = name.toDefaultCodeName(),
    val incompatibleWith: List<KClass<out TestInputTransformer>> = emptyList(),
    val expectFailure: TestConfig.ExpectedFailure? = null,
    /**
     * `true` allows to skip JsonTestMode with this transformer disabled
     * if it was successful when it was enabled.
     */
    val allowSkipDisabledIfSuccess: Boolean = false,
    val deterministic: Boolean = false
) {
    init {
        require(name.isNotEmpty())
        require(name.trim() == name)

        require(code.isNotEmpty())
        require(code.trim() == code)

        require(targets.isNotEmpty())
    }

    // TODO PrepareResult { NotApplicable, Ok }
	abstract fun transform(input: MutableTestInput)
}

private fun String.toDefaultCodeName(): String = buildString {
    val input = this@toDefaultCodeName

    input.splitIterate(' ') { start, end ->
        val length = end - start
        if (length > 0) append(input[start].uppercase())
    }

    // for single word names, we try getting first 2/3 letters
    if (length == 1) {
        if (input.length > 1) append(input[1].uppercase())
        if (input.length > 2) append(input[2].uppercase())
    }
}