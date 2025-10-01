package dev.dokky.zerojson.framework

import karamel.utils.readableName
import kotlin.reflect.KClass

sealed class JsonTestResult {
    abstract val mode: JsonTestMode

    open fun print(output: StringBuilder) {}

    data class Ok(override val mode: JsonTestMode, val skipped: Int): JsonTestResult() {
        override fun print(output: StringBuilder) {
            if (skipped != 0) {
                output.append("Skipped: ").append(skipped)
            }
        }
    }

    data class Excluded(override val mode: JsonTestMode): JsonTestResult()

    data class Skipped(override val mode: JsonTestMode, val reason: String? = null): JsonTestResult() {
        override fun print(output: StringBuilder) {
            if (reason != null) {
                output.append("Reason: ").append(reason)
            }
        }
    }

    abstract class Failure: JsonTestResult() {
        abstract val iteration: Int
        abstract val input: Any?

        protected fun StringBuilder.appendCommon(): StringBuilder {
            append("Iteration: ").appendLine(iteration)
            append("Input: ")
            appendLine(
                when (val input = input) {
                    is String -> input.removeLineBreaks()
                    else -> input
                }
            )
            return this
        }

        data class Simple(
            override val mode: JsonTestMode,
            override val iteration: Int,
            override val input: Any?,
            val exception: Throwable
        ): Failure() {
            override fun print(output: StringBuilder) {
                with(output) {
                    appendCommon()
                    if (exception is DataMismatchException) {
                        append("Expected: ").appendLine(exception.expected)
                        append("Actual:   ").appendLine(exception.actual)
                    } else {
                        append("Exception: ").appendLine(exception.stackTraceToString())
                    }
                }
            }
        }

        data class InvalidException(
            override val mode: JsonTestMode,
            override val iteration: Int,
            override val input: Any?,
            val expected: KClass<*>,
            val actual: Throwable
        ): Failure() {
            override fun print(output: StringBuilder) {
                with(output) {
                    appendCommon()
                    append("Expected: ").appendLine(expected.readableName())
                    append("Actual:   ").appendLine(actual.stackTraceToString())
                }
            }
        }

        data class MissingException(
            override val mode: JsonTestMode,
            override val iteration: Int,
            override val input: Any?,
            val expected: KClass<*>
        ): Failure() {
            override fun print(output: StringBuilder) {
                with(output) {
                    appendCommon()
                    append("Expected an exception of type ")
                    append(expected.readableName())
                    appendLine(" but completed successfully")
                }
            }
        }
    }
}