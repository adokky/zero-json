package dev.dokky.zerojson

import kotlinx.serialization.modules.EmptySerializersModule

val DefaultTestConfiguration = ZeroJsonConfiguration {
    serializersModule = EmptySerializersModule()
    namingStrategy = null
    ignoreUnknownKeys = false
    decodeEnumsCaseInsensitive = false
    useAlternativeNames = true
    explicitNulls = false
    encodeDefaults = false
    allowSpecialFloatingPointValues = false
    allowComments = false
    coerceInputValues = false
    isLenient = true
    classDiscriminator = "type"
    stableDefaultProviders = false
    maxStructureDepth = 60
    maxKeyLengthBytes = 1024
    maxEncodedBytes = 100 * 1024
    maxInlineProperties = 4096
    fullStackTraces = false
    allowTrailingComma = false
    strictJsonPrimitives = true
    structuredMapKeysMode = StructuredMapKeysMode.ESCAPED_STRING
    discriminatorConflict = DiscriminatorConflictDetection.SEALED
    cacheMode = CacheMode.SHARED
}

val TestZeroJson = ZeroJson(DefaultTestConfiguration)

fun TestConfiguration(builder: ZeroJsonBuilder.() -> Unit): ZeroJsonConfiguration =
    ZeroJsonConfiguration(DefaultTestConfiguration, builder)

fun TestZeroJson(builder: ZeroJsonBuilder.() -> Unit): ZeroJson =
    ZeroJson(TestZeroJson, builder)