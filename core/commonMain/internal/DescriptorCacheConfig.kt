package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule

internal class DescriptorCacheConfig(
    val serializersModule: SerializersModule,
    val useAlternativeNames: Boolean,
    val namingStrategy: JsonNamingStrategy?,
    val classDiscriminator: String,
    val decodeEnumsCaseInsensitive: Boolean,
    val ignoreUnknownKeys: Boolean
) {
    constructor(config: ZeroJsonConfiguration): this(
        serializersModule = config.serializersModule,
        useAlternativeNames = config.useAlternativeNames,
        namingStrategy = config.namingStrategy,
        classDiscriminator = config.classDiscriminator,
        decodeEnumsCaseInsensitive = config.decodeEnumsCaseInsensitive,
        ignoreUnknownKeys = config.ignoreUnknownKeys,
    )

    private var _hashCode = 0

    private fun initHashCode(): Int {
        var result = useAlternativeNames.hashCode()
        result = 31 * result + decodeEnumsCaseInsensitive.hashCode()
        result = 31 * result + ignoreUnknownKeys.hashCode()
        result = 31 * result + _hashCode
        result = 31 * result + serializersModule.hashCode()
        result = 31 * result + (namingStrategy?.hashCode() ?: 0)
        result = 31 * result + classDiscriminator.hashCode()
        _hashCode = result
        return result
    }

    override fun hashCode() = if (_hashCode == 0) initHashCode() else _hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DescriptorCacheConfig) return false
        if (_hashCode != 0 && other._hashCode != 0 && _hashCode != other._hashCode) return false

        if (useAlternativeNames != other.useAlternativeNames) return false
        if (decodeEnumsCaseInsensitive != other.decodeEnumsCaseInsensitive) return false
        if (ignoreUnknownKeys != other.ignoreUnknownKeys) return false
        if (serializersModule != other.serializersModule) return false
        if (namingStrategy != other.namingStrategy) return false
        if (classDiscriminator != other.classDiscriminator) return false

        return true
    }
}