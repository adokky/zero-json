package dev.dokky.zerojson

import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule

sealed interface ZeroJsonConfigurationBase {
    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [ZeroJson] instance.
     *
     * @see SerializersModule
     * @see Contextual
     * @see Polymorphic
     */
    val serializersModule: SerializersModule

    /**
     * Specifies [JsonNamingStrategy] that should be used for all properties in classes for serialization and deserialization.
     *
     * `null` by default.
     *
     * This strategy is applied for all entities that have [StructureKind.CLASS].
     */
    val namingStrategy: JsonNamingStrategy?

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     *
     * Example of usage:
     * ```
     * @Serializable
     * data class Project(val name: String)
     * val withUnknownKeys = ZeroJson { ignoreUnknownKeys = true }
     * // Project(name=unknown), "version" is ignored completely
     * println(withUnknownKeys.decodeFromString<Project>("""{"name":"unknown", "version": 2.0}"""))
     * // Fails with "Encountered an unknown key 'version'"
     * ZeroJson.decodeFromString<Project>("""{"name":"unknown", "version": 2.0}""")
     * ```
     *
     * In case you wish to allow unknown properties only for specific class(es),
     * consider using [JsonIgnoreUnknownKeys] annotation instead of this configuration flag.
     *
     * @see JsonIgnoreUnknownKeys
     */
    val ignoreUnknownKeys: Boolean

    /**
     * Enables decoding enum values in a case-insensitive manner.
     * Encoding is not affected by this option.
     *
     * It affects both enum serial names and alternative names (specified with the [JsonNames] annotation).
     * Example of usage:
     * ```
     * enum class E { VALUE_A, @JsonNames("ALTERNATIVE") VALUE_B }
     *
     * @Serializable
     * data class Outer(val enums: List<E>)
     *
     * val json = ZeroJson { decodeEnumsCaseInsensitive = true }
     * // Prints [VALUE_A, VALUE_B]
     * println(json.decodeFromString<Outer>("""{"enums":["Value_A", "alternative"]}""").enums)
     * // Will fail with SerializationException: no such enum as 'Value_A'
     * ZeroJson.decodeFromString<Outer>("""{"enums":["Value_A", "alternative"]}""")
     * ```
     *
     * With this feature enabled, it is no longer possible to decode enum values that have the same name in a lowercase form.
     * The following code will throw a serialization exception:
     * ```
     * enum class CaseSensitiveEnum { One, ONE }
     * val json = ZeroJson { decodeEnumsCaseInsensitive = true }
     * // Fails with SerializationException: The suggested name 'one' for enum value ONE is already one of the names for enum value One
     * json.decodeFromString<CaseSensitiveEnum>("ONE")
     * ```
     */
    val decodeEnumsCaseInsensitive: Boolean

    /**
     * Specifies whether [ZeroJson] instance makes use of [JsonNames] annotation.
     *
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large number of fields is skipped with [ignoreUnknownKeys].
     * `true` by default.
     */
    val useAlternativeNames: Boolean

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     *
     * When this flag is disabled, properties with `null` values are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     *
     * `true` by default.
     *
     * It is possible to make decoder treat some invalid input data as the missing field to enhance the functionality of this flag.
     * See [coerceInputValues] documentation for details.
     *
     * Example of usage:
     * ```
     * @Serializable
     * data class Project(val name: String, val description: String?)
     * val implicitNulls = ZeroJson { explicitNulls = false }
     *
     * // Encoding
     * // Prints '{"name":"unknown","description":null}'. null is explicit
     * println(ZeroJson.encodeToString(Project("unknown", null)))
     * // Prints '{"name":"unknown"}', null is omitted
     * println(implicitNulls.encodeToString(Project("unknown", null)))
     *
     * // Decoding
     * // Prints Project(name=unknown, description=null)
     * println(implicitNulls.decodeFromString<Project>("""{"name":"unknown"}"""))
     * // Fails with "MissingFieldException: Field 'description' is required"
     * ZeroJson.decodeFromString<Project>("""{"name":"unknown"}""")
     * ```
     *
     * Exercise extra caution if you want to use this flag and have non-typical classes with properties
     * that are nullable, but have default value that is not `null`. In that case, encoding and decoding will not
     * be symmetrical if `null` is omitted from the output.
     * Example of such a pitfall:
     *
     * ```
     * @Serializable
     * data class Example(val nullable: String? = "non-null default")
     *
     * val json = ZeroJson { explicitNulls = false }
     *
     * val original = Example(null)
     * val s = json.encodeToString(original)
     * // prints "{}" because of explicitNulls flag
     * println(s)
     * val decoded = json.decodeFromString<Example>(s)
     * // Prints "non-null default" because default value is inserted since `nullable` field is missing in the input
     * println(decoded.nullable)
     * println(decoded != original) // true
     * ```
     */
    val explicitNulls: Boolean

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     *
     * Example:
     * ```
     * @Serializable
     * class Project(val name: String, val language: String = "kotlin")
     *
     * // Prints {"name":"test-project"}
     * println(ZeroJson.encodeToString(Project("test-project")))
     *
     * // Prints {"name":"test-project","language":"kotlin"}
     * val withDefaults = ZeroJson { encodeDefaults = true }
     * println(withDefaults.encodeToString(Project("test-project")))
     * ```
     *
     * This option does not affect decoding.
     */
    val encodeDefaults: Boolean

    /**
     * Removes JSON specification restriction on special floating-point values such as `NaN` and `Infinity`
     * and enables their serialization and deserialization as float literals without quotes.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     * This option affects both encoding and decoding.
     * `false` by default.
     *
     * Example of usage:
     * ```
     * val floats = listOf(1.0, 2.0, Double.NaN, Double.NEGATIVE_INFINITY)
     * val json = ZeroJson { allowSpecialFloatingPointValues = true }
     * // Prints [1.0,2.0,NaN,-Infinity]
     * println(json.encodeToString(floats))
     * // Prints [1.0, 2.0, NaN, -Infinity]
     * println(json.decodeFromString<List<Double>>("[1.0,2.0,NaN,-Infinity]"))
     * ```
     */
    val allowSpecialFloatingPointValues: Boolean

    /**
     * Allows parser to accept C/Java-style comments in JSON input.
     *
     * Comments are being skipped and are not stored anywhere; this setting does not affect encoding in any way.
     *
     * More specifically, a comment is a substring that is not a part of JSON key or value, conforming to one of these:
     *
     * 1. Starts with `//` characters and ends with a newline character `\n`.
     * 2. Starts with `/*` characters and ends with `*/` characters. Nesting block comments
     *    is not supported: no matter how many `/*` characters you have, the first `*/` will end the comment.
     *
     * `false` by default.
     */
    val allowComments: Boolean

    /**
     * Allows parser to accept trailing (ending) commas in JSON objects and arrays,
     * making inputs like `[1, 2, 3,]` and `{"key": "value",}` valid.
     * Does not affect encoding.
     *
     * `false` by default.
     */
    val allowTrailingComma: Boolean

    /**
     * Enables coercing incorrect JSON values in the following cases:
     *
     *   1. JSON value is `null` but the property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains an unknown enum member.
     *
     * Coerced values are treated as missing; they are replaced either with a default property value if it exists, or with a `null` if [explicitNulls] flag
     * is set to `false` and a property is nullable (for enums).
     *
     * Example of usage:
     * ```
     * enum class Choice { A, B, C }
     *
     * @Serializable
     * data class Example1(val a: String = "default", val b: Choice = Choice.A, val c: Choice? = null)
     *
     * val coercingJson = ZeroJson { coerceInputValues = true }
     * // Decodes Example1("default", Choice.A, null) instance
     * coercingJson.decodeFromString<Example1>("""{"a": null, "b": "unknown", "c": "unknown"}""")
     *
     * @Serializable
     * data class Example2(val c: Choice?)
     *
     * val coercingImplicitJson = ZeroJson(coercingJson) { explicitNulls = false }
     * // Decodes Example2(null) instance.
     * coercingImplicitJson.decodeFromString<Example2>("""{"c": "unknown"}""")
     * ```
     *
     * `false` by default.
     */
    val coerceInputValues: Boolean

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode, unquoted JSON keys and string values are allowed.
     *
     * Example of invalid JSON that is accepted with this flag set:
     * `{key: value}` can be parsed into `@Serializable class Data(val key: String)`.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid values in the input.
     *
     * `false` by default.
     */
    val isLenient: Boolean

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * `type` by default.
     *
     * Note that if your class has any serial names that are equal to [classDiscriminator]
     * (e.g., `@Serializable class Foo(val type: String)`), an [IllegalArgumentException] will be thrown from `ZeroJson {}` builder.
     */
    val classDiscriminator: String

    /**
     * If `true`, then `defaultSerializerProvider` and `defaultDeserializerProvider` treated
     * as pure functions (which means they return exact same result for exact same inputs).
     * This may significantly improve performance of polymorphic deserialization involving default providers.
     *
     * Disabled by default to match [SerializersModule] specification.
     */
    val stableDefaultProviders: Boolean

    /**
     * Maximum depth of nested JSON structures allowed during parsing.
     *
     * Example:
     * ```
     * // This would fail with SerializationException
     * val json = """[[[1, 2], [3, 4]], [[5, 6], [7, 8]]]"""
     * ZeroJson { maxStructureDepth = 2 }.decodeFromString<List<List<List<Int>>>>(json)
     * ```
     */
    val maxStructureDepth: Int

    /**
     * Maximum allowed length of JSON object keys in bytes.
     * The limit only applied when decoding from binary source ([io.kodec.buffers.Buffer], [ByteArray], etc.).
     *
     * `1024` by default.
     */
    val maxKeyLengthBytes: Int

    /**
     * Maximum size of JSON output in bytes.
     * The limit only applied when encoding into binary output ([io.kodec.buffers.MutableBuffer], [ByteArray], etc.)
     *
     * `102400` (`100` KiB) by default.
     */
    val maxOutputBytes: Int

    /**
     * Maximum number of inline properties allowed per object.
     * `4096` by default.
     */
    val maxInlineProperties: Int

    /**
     * Specifies whether full stack traces should be included in serialization exceptions.
     * When `false`, stack traces are trimmed to reduce output size and improve performance.
     *
     * his setting does not take any effect if [ZeroJson.captureStackTraces] is `false`.
     *
     * Default: `false`
     */
    val fullStackTraces: Boolean

    /**
     * Controls strictness of [kotlinx.serialization.json.JsonPrimitive] parsing when [isLenient] is `true`.
     *
     * When `true`, unquoted literals parsed as [kotlinx.serialization.json.JsonUnquotedLiteral] when possible.
     * When `false`, unquoted literals parsed as strings when possible.
     *
     * This is compatibility option. Disabling it will match the behaviour of [kotlinx.serialization.json.Json].
     *
     * Default: `true`
     */
    val strictJsonPrimitives: Boolean

    /**
     * Defines how map keys that are not primitive JSON types should be handled.
     *
     * @see StructuredMapKeysMode
     */
    val structuredMapKeysMode: StructuredMapKeysMode

    /**
     * Controls behavior when discriminator field conflicts with regular properties.
     *
     *  Can be overridden by [MaterializedDiscriminator] on a specific classes.
     *
     * Default: [DiscriminatorConflictDetection.SEALED]
     */
    val discriminatorConflict: DiscriminatorConflictDetection

    /**
     * Defines the caching strategy for serialization descriptors.
     *
     * - [CacheMode.SHARED] — Uses a shared cache across all [ZeroJson] instances (default).
     * - [CacheMode.TWO_LEVEL] — Combines thread-local and shared caching for minimizing runtime overheads.
     * - [CacheMode.THREAD_LOCAL] — Uses a thread-local caches across all [ZeroJson] instances.
     *
     * @see CacheMode
     */
    val cacheMode: CacheMode

}

enum class StructuredMapKeysMode {
    /**
     * Completely prevents structured objects in map keys.
     */
    DISABLED,
    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     */
    LIST,
    /**
     * Enables structured objects to be serialized as map keys by
     * converting its serialized form to JSON string.
     */
    ESCAPED_STRING
}

enum class DiscriminatorConflictDetection {
    /**
     * Allows to have explicit discriminator field present in all classes.
     */
    DISABLED,
    /**
     * Allows to have explicit discriminator field present in the all non-sealed classes and its **direct** subclasses.
     *
     * For more granular control set this to `false` and mark specific classes with [MaterializedDiscriminator].
     */
    SEALED,
    /**
     * Prevents explicit discriminator field all classes.
     *
     * For more granular control set this to `false` and mark specific classes with [MaterializedDiscriminator].
     */
    ALL
}

/**
 * Defines the caching strategy for internal data structures used during serialization.
 */
enum class CacheMode {
    /**
     * Shared cache that is used across all threads.
     *
     * Recommended default option that provides good balance between memory usage and performance.
     */
    SHARED,
    /**
     * Two-level cache with thread-local and shared levels.
     *
     * Generally performs worse than [SHARED] due to memory efficiency overhead
     * outweighing runtime benefits. Performance characteristics may change
     * with larger CPU caches and fewer cached descriptors (few serializable classes).
     *
     * Behaves better than [THREAD_LOCAL] in multithreaded scenarios because the descriptors themselves are shared.
     */
    TWO_LEVEL,
    /**
     * Provides the best performance in single-threaded scenarios.
     */
    THREAD_LOCAL
}
