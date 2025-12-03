package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonContext
import io.kodec.buffers.Buffer
import io.kodec.buffers.MutableBuffer
import karamel.utils.assertionsEnabled
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.internal.FormatLanguage
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * The main entry point to work with JSON serialization.
 * It is typically used by constructing an application-specific instance, with configured JSON-specific behaviour
 * and, if necessary, registered in [SerializersModule] custom serializers.
 * `ZeroJson` instance can be configured in its `ZeroJson {}` factory function using [ZeroJsonBuilder].
 * For demonstration purposes or trivial usages, ZeroJson [companion][ZeroJson.Default] can be used instead.
 *
 * Example of usage:
 * ```
 * @Serializable
 * data class Data(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = ZeroJson { ignoreUnknownKeys = true }
 * val instance = Data(42, "some data", buildJsonObject { put("key", "value") })
 *
 * // Plain ZeroJson usage: returns '{"id": 42, "some data", "extensions": {"key": "value" } }'
 * val jsonString: String = json.encodeToString(instance)
 *
 * // Deserialize from string
 * val deserialized: Data = json.decodeFromString<Data>(jsonString)
 *
 * // Deserialize from JsonElement
 * val deserializedFromElement: Data = json.decodeFromJsonElement<Data>(
 *    buildJsonObject {
 *        put("id", 42)
 *        put("data", "some data")
 *        putObject("key") { put("key", "value") }
 *    }
 * )
 *
 *  // Deserialize from string to JSON tree
 * val deserializedElement: JsonElement = json.parseToJsonElement(jsonString)
 * ```
 *
 * `ZeroJson` instance also exposes its [configuration] that can be used in custom serializers
 * that rely on [ZeroJsonDecoder] and [ZeroJsonEncoder] for customizable behaviour.
 *
 * JSON format configuration can be refined using the corresponding constructor:
 * ```
 * val defaultJson = ZeroJson {
 *     encodeDefaults = true
 *     ignoreUnknownKeys = true
 * }
 * // Will inherit the properties of defaultJson
 * val jsonWithComments = ZeroJson(defaultJson) {
 *     allowComments = true
 * }
 * ```
 */
sealed class ZeroJson(val configuration: ZeroJsonConfiguration): StringFormat, BinaryFormat {
    override val serializersModule: SerializersModule = configuration.serializersModule

    /**
     * Deserializes the given JSON UTF-8 string encoded in [input] starting from [offset]
     * into a value of type [T] using the given [deserializer].
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * val encoded = """{"name":"kotlinx.serialization","language":"Kotlin"}"""
     *     .encodeToByteArray().asArrayBuffer()
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(ZeroJson.decodeFromByteArray(Project.serializer(), encoded))
     * ```
     *
     * @throws [ZeroJsonDecodingException] if the given JSON string is not a valid JSON input for the type [T]
     */
    fun <T> decode(deserializer: DeserializationStrategy<T>, input: Buffer, offset: Int = 0): T =
        JsonContext.useThreadLocal(this) { decode(deserializer, input, offset) }

    /**
     * Deserializes the given JSON [input] starting from [offset]
     * into a value of type [T] using the given [deserializer].
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(ZeroJson.decodeFromString(Project.serializer(), """{"name":"kotlinx.serialization","language":"Kotlin"}"""))
     * ```
     *
     * @throws [ZeroJsonDecodingException] if the given JSON string is not a valid JSON input for the type [T]
     */
    fun <T> decode(deserializer: DeserializationStrategy<T>, input: CharSequence, offset: Int = 0): T =
        JsonContext.useThreadLocal(this) { decode(deserializer, input, offset) }

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     * This method is recommended to be used with an explicit serializer (e.g. the custom or third-party one),
     * otherwise the `encode(value: T)` version might be preferred as the most concise one.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     * val destinationBuffer = ArrayBuffer(100)
     *
     * val endPosition = ZeroJson.encode(Project.serializer(), data, destinationBuffer)
     * val encoded = destinationBuffer.toByteArray(endExclusive = endPosition)
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(encoded.decodeToString())
     * ```
     *
     * @return end position (exclusive) = (index of last written byte + 1)
     */
    fun <T> encode(serializer: SerializationStrategy<T>, value: T, output: MutableBuffer, offset: Int = 0): Int =
        JsonContext.useThreadLocal(this) { encode(value, serializer, output, offset) }

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     * This method is recommended to be used with an explicit serializer (e.g. the custom or third-party one),
     * otherwise the `encode(value: T)` version might be preferred as the most concise one.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     * val stringBuilder = StringBuilder()
     *
     * ZeroJson.encode(Project.serializer(), data, stringBuilder)
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(stringBuilder.toString())
     * ```
     *
     * @return end position (exclusive) = (index of last written byte + 1)
     */
    fun <T> encode(serializer: SerializationStrategy<T>, value: T, output: StringBuilder) {
        JsonContext.useThreadLocal(this) { encode(value, serializer, output) }
    }

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     * This method is recommended to be used with an explicit serializer (e.g. the custom or third-party one),
     * otherwise the `encodeToString(value: T)` version might be preferred as the most concise one.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     *
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(ZeroJson.encodeToString(Project.serializer(), data))
     * ```
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String =
        JsonContext.useThreadLocal(this) { encodeToString(serializer, value) }

    /**
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(ZeroJson.decodeFromString(Project.serializer(), """{"name":"kotlinx.serialization","language":"Kotlin"}"""))
     * ```
     *
     * @throws [ZeroJsonDecodingException] if the given JSON string is not a valid JSON input for the type [T]
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        decode(deserializer, string)

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     * The resulting JSON encoded using UTF-8 into [ByteArray].
     * This method is recommended to be used with an explicit serializer (e.g. the custom or third-party one),
     * otherwise the `encodeToString(value: T)` version might be preferred as the most concise one.
     *
     * Example of usage:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String)
     *
     * val data = Project("kotlinx.serialization", "Kotlin")
     * val encoded = ZeroJson.encodeToByteArray(Project.serializer(), data)
     * // Prints {"name":"kotlinx.serialization","language":"Kotlin"}
     * println(encoded.decodeToString())
     * ```
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray =
        JsonContext.useThreadLocal(this) { encodeToByteArray(serializer, value) }

    /**
     * Deserializes the given JSON UTF-8 string encoded in [bytes]
     * into a value of type [T] using the given [deserializer].
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * val encoded = """{"name":"kotlinx.serialization","language":"Kotlin"}""".encodeToByteArray()
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(ZeroJson.decodeFromByteArray(Project.serializer(), encoded))
     * ```
     *
     * @throws [ZeroJsonDecodingException] if the given JSON string is not a valid JSON input for the type [T]
     */
    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T =
        JsonContext.useThreadLocal(this) { decodeFromByteArray(deserializer, bytes, offset = 0) }

    /**
     * Deserializes the given JSON UTF-8 string encoded in [bytes] starting from [offset]
     * into a value of type [T] using the given [deserializer].
     * Example:
     * ```
     * @Serializable
     * data class Project(val name: String, val language: String)
     * val encoded = """{"name":"kotlinx.serialization","language":"Kotlin"}""".encodeToByteArray()
     * //  Project(name=kotlinx.serialization, language=Kotlin)
     * println(ZeroJson.decodeFromByteArray(Project.serializer(), encoded))
     * ```
     *
     * @throws [ZeroJsonDecodingException] if the given JSON string is not a valid JSON input for the type [T]
     */
    fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray, offset: Int): T =
        JsonContext.useThreadLocal(this) { decodeFromByteArray(deserializer, bytes, offset = offset) }

    /**
     * Deserializes the given JSON [string] into a corresponding [JsonElement] representation.
     *
     * @throws [ZeroJsonDecodingException] if the given string is not a valid JSON
     */
    fun parseToJsonElement(@FormatLanguage("json", "", "") string: CharSequence): JsonElement =
        decode(JsonElementSerializer, string)

    /**
     * Deserializes the given JSON UTF-8 string encoded in [bytes] starting from [offset]
     * into a corresponding [JsonElement] representation.
     *
     * @throws [ZeroJsonDecodingException] if the given string is not a valid JSON
     */
    fun parseToJsonElement(bytes: ByteArray, offset: Int = 0): JsonElement =
        decodeFromByteArray(JsonElementSerializer, bytes, offset = offset)

    /**
     * Deserializes the given JSON UTF-8 string encoded in [buffer] starting from [offset]
     * into a corresponding [JsonElement] representation.
     *
     * @throws [ZeroJsonDecodingException] if the given string is not a valid JSON
     */
    fun parseToJsonElement(buffer: Buffer, offset: Int = 0): JsonElement =
        decode(JsonElementSerializer, buffer, offset = offset)

    /**
     * Deserializes the given [element] into a value of type [T] using the given [deserializer].
     *
     * @throws [ZeroJsonDecodingException] if the given JSON element is not a valid JSON input for the type [T]
     */
    fun <T> decodeFromJsonElement(deserializer: DeserializationStrategy<T>, element: JsonElement): T =
        JsonContext.useThreadLocal(this) { decodeFromJsonElement(deserializer, element) }

    /**
     * Serializes the given [value] into an equivalent [JsonElement] using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON
     */
    fun <T> encodeToJsonElement(serializer: SerializationStrategy<T>, value: T): JsonElement =
        JsonContext.useThreadLocal(this) { encodeToJsonElement(serializer, value) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZeroJson) return false
        if (configuration != other.configuration) return false
        return true
    }

    override fun hashCode(): Int = configuration.hashCode()

    override fun toString(): String = "ZeroJson($configuration)"

    internal class Impl(config: ZeroJsonConfiguration): ZeroJson(config) {
        var json: Json? = null
    }

    companion object Default: ZeroJson(ZeroJsonConfiguration.Default) {
        @InternalSerializationApi
        @JvmStatic
        @JvmName("create")
        operator fun invoke(config: ZeroJsonConfiguration): ZeroJson = Impl(config)

        @InternalSerializationApi
        @JvmStatic
        @JvmName("create")
        @JvmOverloads
        operator fun invoke(
            config: JsonConfiguration,
            serializersModule: SerializersModule = EmptySerializersModule()
        ): ZeroJson {
            return Impl(ZeroJsonConfiguration(config, serializersModule))
        }

        @OptIn(ExperimentalAtomicApi::class)
        private val _captureStackTraces = AtomicBoolean(assertionsEnabled)

        /**
         * If `true` the exact stack trace will be preserved for any [SerializationException].
         *
         * Default:
         * * on JVM and Android the assertion flag is used as the default;
         * * on JS, Native it is `false`.
         */
        @OptIn(ExperimentalAtomicApi::class)
        @JvmStatic
        var captureStackTraces: Boolean
            set(value) { _captureStackTraces.store(value) }
            get() = _captureStackTraces.load()
    }

    /**
     * Similar to [ZeroJson.Default] but configured for
     * maximum compatibility with [kotlinx.serialization.json.Json].
     */
    object KtxCompat: ZeroJson(ZeroJsonConfiguration.KotlinxJson)
}

// decode shortcuts

inline fun <reified T> ZeroJson.decode(@FormatLanguage("json", "", "") input: CharSequence): T =
    decode(serializersModule.serializer<T>(), input)

/**
 * A concise equivalent of `decode(serializersModule.serializer<T>(), input)` 
 * with automatically inferred serializer.
 * 
 * @see [ZeroJson.decode]
 */
inline fun <reified T> ZeroJson.decode(input: Buffer): T =
    decode(serializersModule.serializer<T>(), input)

// encode shortcuts

/**
 * A concise equivalent of `encode(serializersModule.serializer<T>(), output)`
 * with automatically inferred deserializer.
 *
 * @see [ZeroJson.encode]
 */
inline fun <reified T> ZeroJson.encode(value: T, output: MutableBuffer): Int =
    encode(serializersModule.serializer<T>(), value, output)

/**
 * Deserializes the given [json] element into a value of type [T] using a deserializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 */
inline fun <reified T> ZeroJson.decodeFromJsonElement(json: JsonElement): T =
    decodeFromJsonElement(serializersModule.serializer<T>(), json)

/**
 * Serializes the given [value] into an equivalent [JsonElement] using the given [serializer].
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON
 */
inline fun <reified T> ZeroJson.encodeToJsonElement(value: T): JsonElement =
    encodeToJsonElement(serializersModule.serializer<T>(), value)

@InternalSerializationApi
fun ZeroJson.setKtxJson(json: Json) {
    (this as? ZeroJson.Impl)?.json = json
}