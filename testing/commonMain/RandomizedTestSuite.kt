package dev.dokky.zerojson.framework

import kotlin.test.fail

class RandomizedTestsDsl internal constructor(
    @PublishedApi internal val context: RandomizedJsonTest,
    private val shortMessage: Boolean,
    private val prettifyException: Boolean
) {
    @PublishedApi internal val tests: LinkedHashMap<String, TestConfig<*>> = LinkedHashMap()

    inline fun test(name: String, builder: TestConfigBuilder.() -> Unit) {
        val cfg = TestConfigBuilder()
            .apply(builder)
            .also {
                require(it.name == null) { "test name can not be changed inside config builder" }
                it.name = name
            }
            .toConfig<Any?>()

        if (tests.put(name, cfg) != null) {
            error("test with name '$name' already registered")
        }
    }

    inline fun <reified T> test(
        name: String,
        domainObject: T,
        crossinline jsonObjectBuilder: DslJsonObjectBuilder.() -> Unit
    ) {
        test(name) {
            this.name = name
            this.domainObject(domainObject)
            jsonElement = jsonObject(buildJson = jsonObjectBuilder)
        }
    }

    internal fun run(): Map<String, TestResult<*>> =
        tests.mapValues { (_, cfg) -> context.doRandomizedTest(cfg) }

    internal fun reportErrors() {
        val results = run()

        val sb = StringBuilder()

        for ((_, result) in results) {
            if (!result.isFailed) continue

            buildReport(result,
                output = sb,
                detailedFailures = !shortMessage,
                showSucceeded = true,
                showSkipped = true,
                showExcluded = true,
                verboseModeName = true
            )
        }

        if (sb.isEmpty()) return

        val report = sb.toString()
        if (prettifyException) prettyFail(report) else fail(report)
    }
}

fun RandomizedJsonTest.randomizedTestSuite(
    shortMessage: Boolean = false,
    prettifyException: Boolean = true,
    body: RandomizedTestsDsl.() -> Unit
) {
    RandomizedTestsDsl(this, shortMessage = shortMessage, prettifyException = prettifyException)
        .apply(body)
        .reportErrors()
}

fun RandomizedJsonTest.doRandomizedTestSuite(
    shortMessage: Boolean = false,
    prettifyException: Boolean = true,
    body: RandomizedTestsDsl.() -> Unit
): Map<String, TestResult<*>> {
    return RandomizedTestsDsl(this, shortMessage = shortMessage, prettifyException = prettifyException)
        .apply(body)
        .run()
}