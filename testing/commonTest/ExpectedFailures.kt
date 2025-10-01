package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.TestTarget.DataType
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExpectedFailures: AbstractRunnerTest() {
    @Test
    fun expect_target_failure() {
        // prettifyException=true uses prettyFail which just prints major part of exception message
        randomizedTest(prettifyException = false) {
            configure(corruption = false)
            expectFailure(TestTarget.entries.filter { it.isDomain() })
        }
        assertFailsWith<AssertionError> {
            randomizedTest(prettifyException = false) {
                configure(corruption = false)
                expectFailure(TestTarget.BinaryToObject, TestTarget.TextToObject)
            }
        }
        assertFailsWith<AssertionError> {
            randomizedTest(prettifyException = false) {
                configure(corruption = false)
                expectFailure(TestTarget.ObjectToText)
            }
        }
        randomizedTest(prettifyException = false) {
            configure(corruption = false, injectEncoderError = false)
            expectFailure(TestTarget.entries.filter { it.output == DataType.Domain })
        }
    }

    @Test
    fun unexpected_failure_type() {
        val result = doRandomizedTest {
            domainObject(simpleData,
                BuggySerializer(decoderFailAt = -1, encoderException = IllegalStateException("test"))
            )
            jsonElement = expectedJsonElement
            includeOnly(TestTarget.ObjectToText)
            expectFailure(TestTarget.ObjectToText)
        }

        assertTrue(result.results.isNotEmpty())
        assertTrue(result.failures().all {
            it is JsonTestResult.Failure.InvalidException && it.actual.message == "test"
        })
    }

    private fun runTestWithInjectedISE(matchMessage: Boolean): TestResult<*> = doRandomizedTest {
        domainObject(
            simpleData, BuggySerializer(
                decoderFailAt = -1,
                encoderException = IllegalStateException("123".takeIf { matchMessage })
            )
        )
        jsonElement = expectedJsonElement
        includeOnly(TestTarget.ObjectToText)
        expectFailureIf<IllegalStateException>(TestTarget.ObjectToText) { it.message == "123" }
    }

    @Test
    fun expected_failure_type_with_custom_condition() {
        runTestWithInjectedISE(matchMessage = true).reportErrors()
    }

    @Test
    fun unexpected_failure_type_with_custom_condition() {
        val result = runTestWithInjectedISE(matchMessage = false)
        val failures = result.failures()
        assertTrue(failures.isNotEmpty())
        assertTrue(failures.all { it is JsonTestResult.Failure.InvalidException })
    }
}