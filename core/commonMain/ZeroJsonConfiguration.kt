package dev.dokky.zerojson

import dev.dokky.zerojson.internal.DescriptorCacheConfig
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.jvm.JvmField

class ZeroJsonConfiguration internal constructor(
    override val serializersModule: SerializersModule = EmptySerializersModule(),
    override val namingStrategy: JsonNamingStrategy? = null,
    override val ignoreUnknownKeys: Boolean = false,
    override val decodeEnumsCaseInsensitive: Boolean = false,
    override val useAlternativeNames: Boolean = true,
    override val explicitNulls: Boolean = true,
    override val encodeDefaults: Boolean = false,
    override val allowSpecialFloatingPointValues: Boolean = false,
    override val allowComments: Boolean = false,
    override val coerceInputValues: Boolean = false,
    override val isLenient: Boolean = false,
    override val classDiscriminator: String = "type",
    override val stableDefaultProviders: Boolean = false,
    override val maxStructureDepth: Int = 60,
    override val maxKeyLengthBytes: Int = 1024,
    override val maxOutputBytes: Int = 100 * 1024,
    override val maxInlineProperties: Int = 4096,
    override val fullStackTraces: Boolean = false,
    override val allowTrailingComma: Boolean = false,
    override val strictJsonPrimitives: Boolean = true,
    override val structuredMapKeysMode: StructuredMapKeysMode = StructuredMapKeysMode.LIST,
    override val discriminatorConflict: DiscriminatorConflictDetection = DiscriminatorConflictDetection.SEALED,
    override val cacheMode: CacheMode = CacheMode.SHARED,
): ZeroJsonConfigurationBase {
    init {
        require(maxStructureDepth in 2..DEPTH_LIMIT)
    }

    internal val descriptorCacheConfig = DescriptorCacheConfig(this)
    internal val polymorphicSerializerCacheKey = PolymorphicSerializerCacheKey(
        descriptorCacheConfig, stableDefaultProviders, discriminatorConflict)

    internal data class PolymorphicSerializerCacheKey(
        val descriptorCacheConfig: DescriptorCacheConfig,
        val stableDefaultProviders: Boolean,
        val discriminatorConflictDetection: DiscriminatorConflictDetection
    )

    private fun computeHashCode(): Int {
        var result = ignoreUnknownKeys.hashCode()
        result = 31 * result + decodeEnumsCaseInsensitive.hashCode()
        result = 31 * result + useAlternativeNames.hashCode()
        result = 31 * result + explicitNulls.hashCode()
        result = 31 * result + encodeDefaults.hashCode()
        result = 31 * result + allowSpecialFloatingPointValues.hashCode()
        result = 31 * result + allowComments.hashCode()
        result = 31 * result + coerceInputValues.hashCode()
        result = 31 * result + isLenient.hashCode()
        result = 31 * result + stableDefaultProviders.hashCode()
        result = 31 * result + maxStructureDepth
        result = 31 * result + maxKeyLengthBytes
        result = 31 * result + maxOutputBytes
        result = 31 * result + maxInlineProperties
        result = 31 * result + fullStackTraces.hashCode()
        result = 31 * result + serializersModule.hashCode()
        result = 31 * result + (namingStrategy?.hashCode() ?: 0)
        result = 31 * result + classDiscriminator.hashCode()
        result = 31 * result + allowTrailingComma.hashCode()
        result = 31 * result + strictJsonPrimitives.hashCode()
        result = 31 * result + structuredMapKeysMode.hashCode()
        result = 31 * result + discriminatorConflict.hashCode()
        result = 31 * result + cacheMode.hashCode()
        return result
    }

    private fun equalsSlow(other: ZeroJsonConfiguration): Boolean {
        if (ignoreUnknownKeys != other.ignoreUnknownKeys) return false
        if (decodeEnumsCaseInsensitive != other.decodeEnumsCaseInsensitive) return false
        if (useAlternativeNames != other.useAlternativeNames) return false
        if (explicitNulls != other.explicitNulls) return false
        if (encodeDefaults != other.encodeDefaults) return false
        if (allowSpecialFloatingPointValues != other.allowSpecialFloatingPointValues) return false
        if (allowComments != other.allowComments) return false
        if (coerceInputValues != other.coerceInputValues) return false
        if (isLenient != other.isLenient) return false
        if (stableDefaultProviders != other.stableDefaultProviders) return false
        if (maxStructureDepth != other.maxStructureDepth) return false
        if (maxKeyLengthBytes != other.maxKeyLengthBytes) return false
        if (maxOutputBytes != other.maxOutputBytes) return false
        if (maxInlineProperties != other.maxInlineProperties) return false
        if (fullStackTraces != other.fullStackTraces) return false
        if (serializersModule != other.serializersModule) return false
        if (namingStrategy != other.namingStrategy) return false
        if (classDiscriminator != other.classDiscriminator) return false
        if (allowTrailingComma != other.allowTrailingComma) return false
        if (strictJsonPrimitives != other.strictJsonPrimitives) return false
        if (structuredMapKeysMode != other.structuredMapKeysMode) return false
        if (discriminatorConflict != other.discriminatorConflict) return false
        if (cacheMode != other.cacheMode) return false
        return true
    }

    // This class used as key for caching JsonContext
    private var _cachedHashCode = 0

    override fun hashCode(): Int {
        if (_cachedHashCode == 0) _cachedHashCode = computeHashCode()
        return _cachedHashCode
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ZeroJsonConfiguration) return false
        if (hashCode() != other.hashCode()) return false
        return equalsSlow(other)
    }

    companion object {
        @JvmField
        val Default: ZeroJsonConfiguration = ZeroJsonConfiguration()

        internal const val DEPTH_LIMIT: Int = 200

        internal const val ELEMENT_INDEX_NUM_BITS = 15

        // WARN: must be a continuous 1-bit sequence
        internal const val MAX_PROPERTY_ELEMENT_INDEX: Int = (0.inv() shl ELEMENT_INDEX_NUM_BITS).inv()
    }
}

@InternalSerializationApi
fun ZeroJsonConfiguration(
    configuration: JsonConfiguration,
    serializersModule: SerializersModule
): ZeroJsonConfiguration = ZeroJsonConfiguration(
    serializersModule = serializersModule,
    namingStrategy = configuration.namingStrategy,
    ignoreUnknownKeys = configuration.ignoreUnknownKeys,
    decodeEnumsCaseInsensitive = configuration.decodeEnumsCaseInsensitive,
    useAlternativeNames = configuration.useAlternativeNames,
    explicitNulls = configuration.explicitNulls,
    encodeDefaults = configuration.encodeDefaults,
    allowSpecialFloatingPointValues = configuration.allowSpecialFloatingPointValues,
    allowComments = configuration.allowComments,
    coerceInputValues = configuration.coerceInputValues,
    isLenient = configuration.isLenient,
    classDiscriminator = configuration.classDiscriminator,
    allowTrailingComma = configuration.allowTrailingComma,
    strictJsonPrimitives = false,
    structuredMapKeysMode = if (configuration.allowStructuredMapKeys) StructuredMapKeysMode.LIST else StructuredMapKeysMode.DISABLED,
    discriminatorConflict = DiscriminatorConflictDetection.SEALED,
)