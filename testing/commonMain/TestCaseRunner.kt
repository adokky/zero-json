package dev.dokky.zerojson.framework

internal class TestCaseRunner(
    private val config: TestConfig<*>,
    private val resultBuilder: TestResultBuilder
) {
    /** @return `true` if test completed successfully */
    fun run(
        mode: JsonTestMode,
        input: TestInputImpl,
        inputObject: Any?,
        iteration: Int,
        body: () -> Unit
    ): Boolean {
        require(iteration > 0)

        val state = resultBuilder.stateOf(mode)
        if (state.isCompleted) return false

        if (mode.target in config.exclude) {
            resultBuilder.exclude(mode)
            return false
        }

        val expectedFailure: TestConfig.ExpectedFailure? =
            config.expectModeFailure.firstOrNull { it.match(mode) }?.failure
                ?: config.expectTargetFailure[mode.target]
                ?: input.expectedFailure

        // optimization: try running without stack trace if we expect an exception
        input.stackTracesEnabled = expectedFailure == null || !expectedFailure.keepStackTrace

        val failure = runAndGetFailure(mode, iteration, input, inputObject, expectedFailure, body)

        if (failure == null) {
            resultBuilder.success(mode, previous = state)
        } else {
            resultBuilder.fail(failure)
        }

        return failure == null
    }

    private fun runAndGetFailure(
        mode: JsonTestMode,
        iteration: Int,
        input: TestInputImpl,
        inputObject: Any?,
        expectedFailure: TestConfig.ExpectedFailure?,
        body: () -> Unit
    ): JsonTestResult.Failure? {
        try {
            body()

            return expectedFailure?.let {
                JsonTestResult.Failure.MissingException(
                    mode = mode,
                    iteration = iteration,
                    input = inputObject,
                    expected = it.exceptionClass
                )
            }
        } catch (e: Throwable) {
            if (expectedFailure == null) {
                return JsonTestResult.Failure.Simple(
                    mode = mode,
                    iteration = iteration,
                    input = inputObject,
                    exception = e
                )
            }

            if (expectedFailure.match(e)) return null

            input.expectedFailure?.let { transformersEF ->
                if (expectedFailure !== transformersEF && transformersEF.match(e)) {
                    // failure was not expected by general configuration
                    // but was expected by some transformers
                    return null
                }
            }

            if (input.stackTracesEnabled) {
                return JsonTestResult.Failure.InvalidException(
                    mode = mode,
                    iteration = iteration,
                    input = inputObject,
                    expected = expectedFailure.exceptionClass,
                    actual = e
                )
            }

            // rerun test with stack traces enabled
            input.stackTracesEnabled = true
            return runAndGetFailure(
                mode = mode,
                iteration = iteration,
                input = input,
                inputObject = inputObject,
                expectedFailure = expectedFailure,
                body = body
            )
        }
    }
}