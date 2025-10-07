@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.buffers.ArrayDataBuffer
import io.kodec.buffers.getStringUtf8ByteSized
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.random.Random

class TestRunner(
    val config: TestConfig<*>,
    stringBuilder: StringBuilder,
    decoderBuffer: ArrayDataBuffer,
    private val encoderBuffer: ArrayDataBuffer
) {
    private val modeIterator = TestModeIterator(config.transformers, config.exclude, maxModes = config.modesPerIteration)
    private val resultBuilder = TestResultBuilderImpl(modeIterator, maxIterations = config.iterations)
    private val testCaseRunner = TestCaseRunner(config, resultBuilder)
    private val inputGenerator = TestInputGenerator(config, stringBuilder, decoderBuffer)

    fun run(): List<JsonTestResult> {
        inputGenerator.reset()
        resultBuilder.clear()

        // TestResultBuilder requires iteration number start from 1
        for (i in 1 .. resultBuilder.maxIterations) {
            val anySuccess = testIteration(i)
            inputGenerator.reset()
            if (!anySuccess) break
        }

        return resultBuilder.complete()
    }

    private fun testIteration(iteration: Int): Boolean {
        require(iteration > 0)

        var anySuccess = false

        // Does not matter if prev and new are equal.
        // If the stack is empty it will be properly initialized anyway.
        var previousTransformers = SelectedTransformers.Empty

        modeIterator.forEachMode { mode ->
            if (trySkipMode(mode)) return@forEachMode

            val input = inputGenerator.generate(previousSelected = previousTransformers, selected = mode.transformers)
            previousTransformers = mode.transformers

            if (input == null) return@forEachMode

            anySuccess = testSingleTarget(mode, input, iteration) || anySuccess
        }

        return anySuccess
    }

    @OptIn(InternalTestingApi::class)
    private val skipMask: Int = run {
        var result = SelectedTransformers.Empty
        config.transformers.forEachIndexed { index, transformer ->
            if (transformer.allowSkipDisabledIfSuccess) {
                result = result.withTransformer(config.transformers, index)
            }
        }
        result.bits
    }

    @OptIn(InternalTestingApi::class)
    private fun trySkipMode(mode: JsonTestMode): Boolean {
        val st = SelectedTransformers(mode.transformers.bits or skipMask)
        return st != mode.transformers &&
            resultBuilder.isCompletedSuccessfully(JsonTestMode(mode.target, st))
    }

    private fun testSingleTarget(
        mode: JsonTestMode,
        input: TestInputImpl,
        iteration: Int
    ): Boolean = when (mode.target) {
        TestTarget.TextToObject   -> testTextToObject   (input, mode, iteration)
        TestTarget.TextToTree     -> testTextToTree     (input, mode, iteration)
        TestTarget.TreeToObject   -> testTreeToObject   (input, mode, iteration)
        TestTarget.ObjectToText   -> testObjectToText   (input, mode, iteration)
        TestTarget.BinaryToObject -> testBinaryToObject (input, mode, iteration)
        TestTarget.BinaryToTree   -> testBinaryToTree   (input, mode, iteration)
        TestTarget.ObjectToBinary -> testObjectToBinary (input, mode, iteration)
        TestTarget.ObjectToTree   -> testObjectToTree   (input, mode, iteration)
    }

    private fun testObjectToText(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean =
        testCaseRunner.run(mode, input, input.domainObject, iteration) {
            val encoded = input.json.encodeToString(input.serializer, input.domainObject)
            val actual = Json.parseToJsonElement(encoded)
            assertEncoded(input, actual)
        }

    private fun testObjectToBinary(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean =
        testCaseRunner.run(mode, input, input.domainObject, iteration) {
            val offset = Random.nextInt(7)
            val end = input.json.encode(input.serializer, input.domainObject, encoderBuffer, offset = offset)
            val inputBuf = encoderBuffer.subBuffer(0, end)
            val actual = try {
                input.json.parseToJsonElement(inputBuf, offset = offset)
            } catch (_: ZeroJsonDecodingException) {
                throw TestTargetFailure("encoder produced malformed JSON: \n${inputBuf.getStringUtf8ByteSized(offset, end - offset)}")
            }
            assertEncoded(input, actual)
        }

    private fun testObjectToTree(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean =
        testCaseRunner.run(mode, input, input.domainObject, iteration) {
            val encoded = input.json.encodeToJsonElement(input.serializer, input.domainObject)
            assertEncoded(input, encoded)
        }

    private fun testTreeToObject(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean =
        testCaseRunner.run(mode, input, input.jsonElement, iteration) {
            val actual = input.json.decodeFromJsonElement(input.serializer, input.jsonElement)
            compareObjects(input.domainObject, actual)
        }

    private fun testTextToTree(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean {
        val string = input.stringInput
        return testCaseRunner.run(mode, input, string, iteration = iteration) {
            compareObjects(input.jsonElement, input.json.parseToJsonElement(string))
        }
    }

    private fun testBinaryToTree(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean =
        testCaseRunner.run(mode, input, input.stringInput, iteration) {
            compareObjects(
                input.jsonElement,
                input.json.parseToJsonElement(input.binaryInput, offset = input.binaryOffset)
            )
        }

    private fun testTextToObject(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean {
        val string = input.stringInput
        return testCaseRunner.run(mode, input, string, iteration = iteration) {
            compareObjects(input.domainObject, input.json.decodeFromString(input.serializer, string))
        }
    }

    private fun testBinaryToObject(input: TestInputImpl, mode: JsonTestMode, iteration: Int): Boolean =
        testCaseRunner.run(mode, input, input.stringInput, iteration = iteration) {
            compareObjects(
                input.domainObject,
                input.json.decode(input.serializer, input.binaryInput, offset = input.binaryOffset)
            )
        }

    private fun assertEncoded(input: TestInputImpl, actual: JsonElement) {
        compareObjects(expected = input.jsonElementForEncoding(), actual = actual)
    }

    private fun TestInputImpl.jsonElementForEncoding(): JsonElement = when {
        json.configuration.explicitNulls -> jsonElement
        else -> jsonElementWithoutNullKeys
    }

    private fun compareObjects(expected: Any?, actual: Any?) {
        if (config.compareToString) {
            assertEquals(expected.toString(), actual.toString())
        } else {
            assertEquals(expected, actual)
        }
    }

    private fun <T> assertEquals(expected: T, actual: T) {
        if (expected != actual) throw DataMismatchException(expected, actual)
    }
}

open class TestTargetFailure(
    message: String? = null,
    cause: Throwable? = null
) : AssertionError(message, cause)

open class DataMismatchException(
    val expected: Any?,
    val actual: Any?,
    message: String? = "Expected <$expected>, actual <$actual>"
) : TestTargetFailure(message)
