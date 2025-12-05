package dev.dokky.zerojson.internal

import java.util.concurrent.ConcurrentHashMap

internal actual fun createSharedCaches(): SharedDescriptorCaches? = object : SharedDescriptorCaches {
    // TODO ConcurrentHashMap.putAll is not atomic.
    //  This does not break thread-safety or consistency,
    //  but may cause redundant descriptor instantiations, slightly increasing bootstrap time.
    private var caches = ConcurrentHashMap<DescriptorCacheConfig, DescriptorMap>()

    // It's acceptable to recreate the cache multiple times under race conditions.
    override fun getOrCreate(config: DescriptorCacheConfig): DescriptorMap? =
        caches.getOrPut(config) { ConcurrentHashMap(DEFAULT_DESCRIPTOR_MAP_CAPACITY) }

    override fun clear() {
        caches.clear()
        caches = ConcurrentHashMap<DescriptorCacheConfig, DescriptorMap>()
    }
}
