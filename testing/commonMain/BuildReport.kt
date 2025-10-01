package dev.dokky.zerojson.framework

internal fun buildReport(
    testResult: TestResult<*>,
    output: StringBuilder,
    detailedFailures: Boolean = true,
    showSucceeded: Boolean = true,
    showSkipped: Boolean = true,
    showExcluded: Boolean = false,
    verboseModeName: Boolean = true
) {
    val testResults = testResult.results

    val failed = testResults.filterIsInstance<JsonTestResult.Failure>()
    val succeed = testResults.filterIsInstance<JsonTestResult.Ok>()
    val excluded = testResults.filterIsInstance<JsonTestResult.Excluded>()
    val skipped = testResults.filterIsInstance<JsonTestResult.Skipped>()

    val successRatio = succeed.size.toDouble() / testResults.size

    output.apply {
        if (testResult.config.name != null) append("Test: ").appendLine(testResult.config.name)

        fun printModes(kind: String, results: List<JsonTestResult>) {
            if (results.isEmpty()) return
            append(kind).append(" (").append(results.size).append("): \n")
            appendModes(results, testResult)
            appendLine()
        }

        // short format when observing small a number of failures
        if (failed.isNotEmpty() && successRatio > 0.8 && (skipped.isEmpty() || !showSkipped)) {
            append("Failed (").append(failed.size).append(" / ").append(succeed.size + failed.size).append("): \n")
            appendModes(failed, testResult)
        } else {
            printModes("Failed", failed)

            if (showSucceeded) {
                if (successRatio > 0.8) {
                    append("Success (").append(succeed.size).append(')').appendLine()
                } else {
                    printModes("Success", succeed)
                }
            }
        }

        if (showSkipped) printModes("Skipped", skipped)
        if (showExcluded) printModes("Excluded", excluded)

        appendLine()

        if (detailedFailures) {
            printFailures(failed, testResult, verboseModeName)
        }
    }
}

// {"^rnd_key_0":{"^rnd_key_0":-71869033,"^rnd_key_1":false},"key":"framework"}

private fun StringBuilder.printFailures(
    failed: List<JsonTestResult.Failure>,
    testResult: TestResult<*>,
    verboseModeName: Boolean
) {
    val name = testResult.config.name
    val compactName = name?.all { it.isLetterOrDigit() || it == '_' } == true

    for (result in failed.asReversed()) {
        if (name != null) {
            if (compactName) {
                append("Mode: ")
                append(name).append('/')
            } else {
                append("Group: ").appendLine(name)
                append("Mode: ")
            }
        }

        result.mode.print(testResult, this, verbose = verboseModeName)
        appendLine()

        result.print(this)

        val extraLine = get(length - 1) != '\n'
        if (get(length - 2) != '\n') appendLine()
        if (extraLine) appendLine()
    }
}

internal fun buildReport(
    testResult: TestResult<*>,
    detailedFailures: Boolean = true,
    showSucceeded: Boolean = true,
    showSkipped: Boolean = true,
    showExcluded: Boolean = false,
    verboseModeName: Boolean = true
): String {
    return buildString {
        buildReport(
            testResult,
            output = this,
            detailedFailures = detailedFailures,
            showSucceeded = showSucceeded,
            showSkipped = showSkipped,
            showExcluded = showExcluded,
            verboseModeName = verboseModeName
        )
    }
}

private val leftColonWidth: Int = run {
    val maxTargetNameLength = TestTarget.entries.maxOf { it.name.length }
    maxTargetNameLength + 12
}

@OptIn(InternalTestingApi::class)
private fun StringBuilder.appendModes(results: List<JsonTestResult>, testResult: TestResult<*>) {
    val byTarget = results.groupBy { it.mode.target }

    var needNl = false

    for ((target, modes) in byTarget) {
        if (needNl) appendLine() else needNl = true

        // print test target
        val lineStart = length
        append("    ")
        append(target)
        if (byTarget.size > 1) append(" (").append(modes.size).append(")")
        append(": ")
        repeat(leftColonWidth - (length - lineStart)) { append(' ') }

        // transformers present in all test modes
        if (modes.size > 3) {
            modes
                .map { it.mode.transformers.bits }
                .reduce(Int::and)
                .let(::SelectedTransformers)
                .takeUnless { it.isEmpty }
                ?.let {
                    append('{')
                    it.print(testResult, this, verbose = false)
                    append("}, ")
                }
        }

        if (modes.size > 600) {
            append("...")
            continue
        }

        // reverse, because framework tends to iterate modes from
        // most complex (many transformers) to most simple (no transformers)
        var first = true
        for (fail in modes.asReversed()) {
            if (first) first = false else append(", ")
            append('[')
            fail.mode.transformers.print(testResult, this, verbose = false)
            append(']')
        }
    }
}