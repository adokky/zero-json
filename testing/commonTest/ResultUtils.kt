package dev.dokky.zerojson.framework

internal fun TestResult<*>.failures(): Collection<JsonTestResult.Failure> =
    results.filterIsInstance<JsonTestResult.Failure>()

internal fun TestResult<*>.failedModes(): Set<JsonTestMode> =
    results.filterIsInstance<JsonTestResult.Failure>().map { it.mode }.toSet()

internal fun TestResult<*>.successModes(): Set<JsonTestMode> =
    results.filterIsInstance<JsonTestResult.Ok>().map { it.mode }.toSet()

internal fun TestResult<*>.skippedModes(): Set<JsonTestMode> =
    results.filterIsInstance<JsonTestResult.Skipped>().map { it.mode }.toSet()

internal fun TestResult<*>.excludedModes(): Set<JsonTestMode> =
    results.filterIsInstance<JsonTestResult.Excluded>().map { it.mode }.toSet()

internal fun TestResult<*>.nonSuccesses(): Set<JsonTestMode> =
    results.filter { it !is JsonTestResult.Ok }.map { it.mode }.toSet()