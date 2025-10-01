@file:Suppress("OPT_IN_USAGE")
@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJsonConfiguration
import io.kodec.buffers.ArrayDataBuffer
import kotlinx.serialization.InternalSerializationApi

abstract class RandomizedJsonTest(
    config: ZeroJsonConfiguration = ZeroJsonConfiguration.Default,
    decoderBufferSize: Int = 50 * 1024,
    encoderBufferSize: Int = 10 * 1024
): AbstractDecoderTest(config) {
    private val decoderBuffer = ArrayDataBuffer(decoderBufferSize)
    private val encoderBuffer = ArrayDataBuffer(encoderBufferSize)
    private val stringBuilder = StringBuilder()

    fun <T> doRandomizedTest(config: TestConfig<T>): TestResult<T> {
        val results = TestRunner(config, stringBuilder,
            decoderBuffer = decoderBuffer,
            encoderBuffer = encoderBuffer
        ).run()
        return TestResult(config, results)
    }
}

fun RandomizedJsonTest.randomizedTest(
    config: TestConfig<*>,
    shortMessage: Boolean = false,
    prettifyException: Boolean = true
) {
    doRandomizedTest(config).reportErrors(short = shortMessage, prettifyException = prettifyException)
}

inline fun RandomizedJsonTest.doRandomizedTest(builder: TestConfigBuilder.() -> Unit): TestResult<*> =
    doRandomizedTest(TestConfigBuilder().apply(builder).toConfig<Any?>())

inline fun RandomizedJsonTest.randomizedTest(
    shortMessage: Boolean = false,
    prettifyException: Boolean = true,
    builder: TestConfigBuilder.() -> Unit
) {
    randomizedTest(
        TestConfigBuilder().apply(builder).toConfig<Any?>(),
        shortMessage = shortMessage,
        prettifyException = prettifyException
    )
}

inline fun <reified T> RandomizedJsonTest.randomizedTest(
    domainObject: T,
    name: String? = null,
    crossinline jsonObjectBuilder: DslJsonObjectBuilder.() -> Unit
) {
    randomizedTest {
        this.name = name
        this.domainObject(domainObject)
        jsonElement = jsonObject(buildJson = jsonObjectBuilder)
    }
}

