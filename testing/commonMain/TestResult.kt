package dev.dokky.zerojson.framework

import kotlin.test.fail

class TestResult<T> internal constructor(
    val config: TestConfig<T>,
    val results: List<JsonTestResult>
) {
    fun reportToString(short: Boolean = false): String = buildReport(
        this,
        detailedFailures = !short,
        showSucceeded = true,
        showSkipped = true,
        showExcluded = true,
        verboseModeName = true
    )

    fun reportErrors(short: Boolean = false, prettifyException: Boolean = true) {
        if (results.any { it is JsonTestResult.Failure }) {
            val report = reportToString(short = short)
            if (prettifyException) prettyFail(report) else fail(report)
        }
    }

    val isFailed: Boolean by lazy {
        results.any { it is JsonTestResult.Failure }
    }
}