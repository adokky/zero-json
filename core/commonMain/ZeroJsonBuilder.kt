package dev.dokky.zerojson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder

inline fun ZeroJson(
    prototype: ZeroJson = ZeroJson.Default,
    builder: ZeroJsonBuilder.() -> Unit
): ZeroJson =
    ZeroJson(ZeroJsonConfiguration(prototype.configuration, builder))

inline fun ZeroJsonConfiguration(
    prototype: ZeroJsonConfigurationBase = ZeroJson.configuration,
    builder: ZeroJsonBuilder.() -> Unit
): ZeroJsonConfiguration =
    ZeroJsonBuilder(prototype).apply(builder).toConfig()

@ExperimentalSerializationApi
fun ZeroJson(configuration: JsonConfiguration, serializersModule: SerializersModule): ZeroJson =
    ZeroJson(ZeroJsonConfiguration(configuration, serializersModule))

class ZeroJsonBuilder @PublishedApi internal constructor(config: ZeroJsonConfigurationBase): ZeroJsonConfigurationBase {
    override var serializersModule: SerializersModule         = config.serializersModule
    override var namingStrategy: JsonNamingStrategy?          = config.namingStrategy
    override var ignoreUnknownKeys: Boolean                   = config.ignoreUnknownKeys
    override var decodeEnumsCaseInsensitive: Boolean          = config.decodeEnumsCaseInsensitive
    override var useAlternativeNames: Boolean                 = config.useAlternativeNames
    override var explicitNulls: Boolean                       = config.explicitNulls
    override var encodeDefaults: Boolean                      = config.encodeDefaults
    override var allowComments: Boolean                       = config.allowComments
    @ExperimentalSerializationApi
    override var allowTrailingComma: Boolean                  = config.allowTrailingComma
    override var coerceInputValues: Boolean                   = config.coerceInputValues
    override var isLenient: Boolean                           = config.isLenient
    override var allowSpecialFloatingPointValues: Boolean     = config.allowSpecialFloatingPointValues
    override var classDiscriminator: String                   = config.classDiscriminator
    override var stableDefaultProviders: Boolean              = config.stableDefaultProviders
    override var maxStructureDepth: Int                       = config.maxStructureDepth
    override var maxKeyLengthBytes: Int                       = config.maxKeyLengthBytes
    override var maxOutputBytes: Int                          = config.maxOutputBytes
    override var maxInlineProperties: Int                     = config.maxInlineProperties
    override var fullStackTraces: Boolean                     = config.fullStackTraces
    override var strictJsonPrimitives: Boolean                = config.strictJsonPrimitives
    override var structuredMapKeysMode: StructuredMapKeysMode = config.structuredMapKeysMode
    override var discriminatorConflict: DiscriminatorConflictDetection = config.discriminatorConflict
    override var cacheMode: CacheMode                         = config.cacheMode

    inline fun serializersModule(builder: SerializersModuleBuilder.() -> Unit) {
        serializersModule = SerializersModule(builder)
    }

    @PublishedApi
    internal fun toConfig(): ZeroJsonConfiguration = ZeroJsonConfiguration(
        serializersModule = serializersModule,
        namingStrategy = namingStrategy,
        ignoreUnknownKeys = ignoreUnknownKeys,
        decodeEnumsCaseInsensitive = decodeEnumsCaseInsensitive,
        useAlternativeNames = useAlternativeNames,
        explicitNulls = explicitNulls,
        encodeDefaults = encodeDefaults,
        allowSpecialFloatingPointValues = allowSpecialFloatingPointValues,
        allowComments = allowComments,
        allowTrailingComma = allowTrailingComma,
        coerceInputValues = coerceInputValues,
        isLenient = isLenient,
        classDiscriminator = classDiscriminator,
        stableDefaultProviders = stableDefaultProviders,
        maxStructureDepth = maxStructureDepth,
        maxKeyLengthBytes = maxKeyLengthBytes,
        maxOutputBytes = maxOutputBytes,
        maxInlineProperties = maxInlineProperties,
        fullStackTraces = fullStackTraces,
        strictJsonPrimitives = strictJsonPrimitives,
        structuredMapKeysMode = structuredMapKeysMode,
        discriminatorConflict = discriminatorConflict,
        cacheMode = cacheMode,
    )
}