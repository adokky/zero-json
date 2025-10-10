package dev.dokky.zerojson.internal

import java.util.concurrent.ConcurrentHashMap

internal actual fun createSharedCaches(): SharedDescriptorCaches? = object : SharedDescriptorCaches {
    private val caches = ConcurrentHashMap<DescriptorCacheConfig, DescriptorMap>()

    // does not really matter if we will recreate caches multiple time on some race condition
    override fun getOrCreate(config: DescriptorCacheConfig): DescriptorMap? =
        caches.getOrPut(config) { ConcurrentHashMap(DEFAULT_DESCRIPTOR_MAP_CAPACITY) }
}