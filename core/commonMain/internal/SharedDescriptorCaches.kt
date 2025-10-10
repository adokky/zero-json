package dev.dokky.zerojson.internal

import kotlinx.serialization.descriptors.SerialDescriptor

internal typealias DescriptorMap = MutableMap<SerialDescriptor, ZeroJsonDescriptor>

internal const val DEFAULT_DESCRIPTOR_MAP_CAPACITY = 256

internal interface SharedDescriptorCaches {
    fun getOrCreate(config: DescriptorCacheConfig): DescriptorMap?
}

internal class SimpleSharedCaches: SharedDescriptorCaches {
    private val caches = HashMap<DescriptorCacheConfig, DescriptorMap>()

    override fun getOrCreate(config: DescriptorCacheConfig): DescriptorMap =
        caches.getOrPut(config) { HashMap(DEFAULT_DESCRIPTOR_MAP_CAPACITY) }
}

internal expect fun createSharedCaches(): SharedDescriptorCaches?