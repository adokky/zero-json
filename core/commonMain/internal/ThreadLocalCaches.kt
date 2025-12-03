package dev.dokky.zerojson.internal

import dev.dokky.pool.SimpleObjectPool
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonConfiguration
import karamel.utils.ThreadLocal
import kotlin.jvm.JvmStatic

internal class ThreadLocalCaches private constructor() {
    private val contextPools = HashMap<ZeroJsonConfiguration, SimpleObjectPool<JsonContext>>()
    private val configToDescriptorCache = HashMap<DescriptorCacheConfig, DescriptorCache>()
    private val keyToPolymorphicSerializerCache = HashMap<ZeroJsonConfiguration.PolymorphicSerializerCacheKey, PolymorphicSerializerCache>()

    fun getOrCreateDescriptorCache(config: DescriptorCacheConfig): DescriptorCache =
        configToDescriptorCache.getOrPut(config) {
            DescriptorCache(config)
        }

    private fun getOrCreatePolymorphicSerializerCache(
        config: ZeroJsonConfiguration,
        descriptorCache: DescriptorCache
    ): PolymorphicSerializerCache {
        debugAssert { descriptorCache.config == config.descriptorCacheConfig }
        return keyToPolymorphicSerializerCache.getOrPut(config.polymorphicSerializerCacheKey) {
            PolymorphicSerializerCache(
                config.serializersModule, descriptorCache,
                stableDefaultProviders = config.stableDefaultProviders,
                discriminatorConflictDetection = config.discriminatorConflict
            )
        }
    }

    fun getOrCreateContextPool(json: ZeroJson): SimpleObjectPool<JsonContext> {
        return contextPools.getOrPut(json.configuration) {
            val descriptorCache = getOrCreateDescriptorCache(json.configuration.descriptorCacheConfig)
            val polymorphicSerializerCache = getOrCreatePolymorphicSerializerCache(json.configuration, descriptorCache)
            val ktxJson = (json as? ZeroJson.Impl)?.json
            object : SimpleObjectPool<JsonContext>(1..5) {
                override fun allocate() = JsonContext(
                    this, json, ktxJson, descriptorCache, polymorphicSerializerCache)
            }
        }
    }

    companion object {
        private val threadLocal = ThreadLocal(::ThreadLocalCaches)

        @JvmStatic fun getInstance(): ThreadLocalCaches = threadLocal.get()
    }
}