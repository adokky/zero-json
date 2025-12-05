package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonCompat
import kotlinx.serialization.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue


abstract class JsonTestBase {
    protected val default = ZeroJsonCompat { encodeDefaults = true; isLenient = false }
    protected val lenient = ZeroJsonCompat { ignoreUnknownKeys = true; allowSpecialFloatingPointValues = true; isLenient = true }

    private enum class JsonTestingMode { STRING, BINARY }

    private var mode = JsonTestingMode.STRING

    protected fun parametrizedTest(test: () -> Unit) {
        for (mode in JsonTestingMode.entries) {
            this.mode = mode
            try {
                test()
            } catch (e: Throwable) {
                throw AssertionError("failed mode $mode", e)
            }
        }
    }

    protected fun parametrizedTest(json: ZeroJson, test: StringFormat.() -> Unit) {
        for (mode in JsonTestingMode.entries) {
            this.mode = mode
            try {
                json.test()
            } catch (e: Throwable) {
                throw AssertionError("failed mode $mode", e)
            }
        }
    }

    protected fun <T> ZeroJson.encodeToStringTest(
        serializer: SerializationStrategy<T>,
        value: T
    ): String {
        return if (mode == JsonTestingMode.STRING) {
            encodeToString(serializer, value)
        } else {
            encodeToByteArray(serializer, value).decodeToString()
        }
    }

    protected fun <T> ZeroJson.decodeFromStringTest(
        deserializer: DeserializationStrategy<T>,
        string: String
    ): T {
        return if (mode == JsonTestingMode.STRING) {
            decodeFromString(deserializer, string)
        } else {
            decodeFromByteArray(deserializer, string.encodeToByteArray())
        }
    }

    protected inline fun <reified T> ZeroJson.encodeToStringTest(value: T): String =
        encodeToStringTest(serializersModule.serializer<T>(), value)

    protected inline fun <reified T> ZeroJson.decodeFromStringTest(string: String): T =
        decodeFromStringTest(serializersModule.serializer<T>(), string)

    internal fun <T> assertJsonFormAndRestored(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        json: ZeroJson = default
    ) {
        parametrizedTest {
            val serialized = json.encodeToStringTest(serializer, data)
            assertEquals(expected, serialized)
            val deserialized: T = json.decodeFromStringTest(serializer, expected)
            assertEquals(data, deserialized)
        }
    }

    internal fun <T> assertJsonFormAndRestoredCustom(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        check: (T, T) -> Boolean
    ) {
        parametrizedTest {
            val serialized = TestZeroJson.encodeToStringTest(serializer, data)
            assertEquals(expected, serialized)

            val deserialized: T = TestZeroJson.decodeFromStringTest(serializer, serialized)
            assertTrue("source value =$data\n\tdeserialized value=$deserialized") {
                check(data, deserialized)
            }
        }
    }
}
