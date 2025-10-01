package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonConfiguration
import dev.dokky.zerojson.ZeroJsonDecodingException
import dev.dokky.zerojson.decodeFromJsonElement
import io.kodec.buffers.ArrayDataBuffer
import io.kodec.buffers.asDataBuffer
import karamel.utils.readableName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.internal.FormatLanguage
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
abstract class AbstractDecoderTest(config: ZeroJsonConfiguration = ZeroJsonConfiguration.Default) {
    protected val zjson: ZeroJson = ZeroJson.Default(config)
    protected val ktxJson: Json = Json(config)

    protected fun String.encodeToArrayBuffer(): ArrayDataBuffer =
        encodeToByteArray().asDataBuffer()

    protected inline fun <reified T: Any> encodeDecode(
        value: T,
        compareWithKtx: Boolean = true,
        printEncoded: Boolean = false
    ) {
        val ser = serializer<T>()
        encodeDecode(value, ser, compareWithKtx = compareWithKtx, printEncoded = printEncoded)
    }

    protected fun <T : Any> encodeDecode(
        value: T,
        ser: KSerializer<T>,
        compareWithKtx: Boolean = true,
        printEncoded: Boolean = false
    ) {
        val encodedString = zjson.encodeToString(ser, value)
        if (printEncoded) println("[Encoded]: $encodedString")
        if (compareWithKtx) {
            val expected = ktxJson.encodeToString(ser, value)
            assertEquals(expected, encodedString)
        }
        return assertDecoded(value, ser, encodedString)
    }

    protected inline fun <reified T> assertDecoded(encodedString: String, value: T, json: ZeroJson = zjson) {
        val ser = serializer<T>()
        assertDecoded(value, ser, encodedString, json)
    }

    protected fun <T> assertDecoded(
        value: T,
        ser: KSerializer<T>,
        encodedString: String,
        json: ZeroJson = zjson
    ) {
        for (binary in arrayOf(false, true)) {
            json.assertDecoded(encodedString, ser, value, binary)
        }
    }

    protected fun <T> assertDecoded(
        value: T,
        ser: KSerializer<T>,
        jsonElement: JsonElement,
        json: ZeroJson = zjson
    ) {
        val encodedString = jsonElement.toString()

        for (binary in arrayOf(false, true)) {
            json.assertDecoded(encodedString, ser, value, binary)
        }

        assertDecodedEquals(
            expected = value,
            actual = json.decodeFromJsonElement(ser, jsonElement),
            encodedString = encodedString
        )
    }

    protected fun assertDecodingFails(
        ser: KSerializer<*>,
        jsonElement: JsonElement,
        message: String? = null,
        exception: KClass<out Throwable> = ZeroJsonDecodingException::class,
        json: ZeroJson = zjson
    ) {
        fun check(testName: String, body: () -> Unit) {
            val ex = assertFails(testName) { body() }

            if (!exception.isInstance(ex)) fail(
                "[$testName] Exception type mismatch:\n" +
                "    Expected: ${exception.readableName()}\n" +
                "    Actual:   ${ex.stackTraceToString()}"
            )

            val actualMessage = ex.message
            if (message != null && (actualMessage == null || message !in actualMessage)) fail(
                "[$testName] Exception message mismatch:\n" +
                "Expected: $message\n" +
                "Actual:   ${ex.message}"
            )
        }

        val encodedString = jsonElement.toString()

        check("source=text") { json.decode(ser, encodedString) }
        check("source=binary") { json.decode(ser, encodedString.encodeToArrayBuffer()) }
        check("source=tree") { json.decodeFromJsonElement(ser, jsonElement) }
    }

    protected inline fun <reified T> assertDecodingFails(
        jsonElement: JsonElement,
        message: String? = null,
        exception: KClass<out Throwable> = ZeroJsonDecodingException::class,
        json: ZeroJson = zjson
    ): Unit = assertDecodingFails(serializer<T>(), jsonElement, message, exception, json)

    protected inline fun <reified T> assertDecoded(value: T, jsonElement: JsonElement, json: ZeroJson = zjson): Unit =
        assertDecoded(value, serializer<T>(), jsonElement, json)

    private fun <T> ZeroJson.assertDecoded(
        encodedString: String,
        ser: KSerializer<T>,
        value: T,
        binary: Boolean
    ) {
        val decoded = if (binary) {
            decode(ser, encodedString.encodeToArrayBuffer())
        } else {
            decode(ser, encodedString)
        }

        assertDecodedEquals(expected = value, actual = decoded, encodedString = encodedString)
    }

    protected fun <T> assertDecodedEquals(expected: T, actual: T, encodedString: String) {
        if (expected != actual) fail(
            """              
                expected: $expected
                actual: $actual
                encoded: $encodedString
            """.trimIndent()
        )
    }

    protected fun <T : Any> assertDecodedEquals(
        @FormatLanguage("json", "", "") json: String,
        expected: T,
        ser: KSerializer<T>
    ) {
        zjson.assertDecodedEquals(json, expected, ser)
    }

    protected fun <T : Any> ZeroJson.assertDecodedEquals(
        @FormatLanguage("json", "", "") json: String,
        expected: T,
        ser: KSerializer<T>
    ) {
        val decoded = decode(ser, json)
        assertEquals(expected, decoded)
    }

    protected inline fun <reified T : Any> assertDecodedEquals(
        @FormatLanguage("json", "", "") json: String,
        expected: T,
        testTreeDecoder: Boolean = false,
    ) {
        zjson.assertDecodedEquals(json, expected, testTreeDecoder = testTreeDecoder)
    }

    protected inline fun <reified T : Any> ZeroJson.assertDecodedEquals(
        @FormatLanguage("json", "", "") json: String,
        expected: T,
        testTreeDecoder: Boolean = false,
    ) {
        assertDecodedEquals(json, expected, serializer())

        if (testTreeDecoder) {
            val tree = parseToJsonElement(json)
            val obj = decodeFromJsonElement<T>(tree)
            assertEquals(expected, obj, "JsonTextDecoder succeeded, JsonTreeDecoder failed")
        }
    }

    @Suppress("OPT_IN_USAGE")
    protected fun Json(config: ZeroJsonConfiguration): Json = Json {
        serializersModule = config.serializersModule
        namingStrategy = config.namingStrategy
        ignoreUnknownKeys = config.ignoreUnknownKeys
        decodeEnumsCaseInsensitive = config.decodeEnumsCaseInsensitive
        useAlternativeNames = config.useAlternativeNames
        explicitNulls = config.explicitNulls
        encodeDefaults = config.encodeDefaults
        allowSpecialFloatingPointValues = config.allowSpecialFloatingPointValues
        allowComments = config.allowComments
        coerceInputValues = config.coerceInputValues
        isLenient = config.isLenient
        classDiscriminator = config.classDiscriminator
        useArrayPolymorphism = false
    }
}

