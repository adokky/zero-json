package dev.dokky.zerojson

import dev.dokky.pool.use
import dev.dokky.zerojson.internal.DescriptorCache
import dev.dokky.zerojson.internal.JsonContext
import dev.dokky.zerojson.internal.PolymorphicSerializerCache
import io.kodec.text.substringWrapper
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PolymorphicSerializerCacheTest {
    private val serializersModule = SerializersModule {
        polymorphic(Any::class) {
            subclass(Int::class, Int.serializer())
            subclass(SimpleValueInteger::class, SimpleValueInteger.serializer())
        }

        polymorphicDefaultDeserializer(Any::class) { Long.serializer() }
    }

    private val descriptorCache = DescriptorCache(ZeroJsonConfiguration.Default.descriptorCacheConfig)
    private val cache = PolymorphicSerializerCache(
        serializersModule, descriptorCache,
        stableDefaultProviders = ZeroJsonConfiguration.Default.stableDefaultProviders,
        discriminatorConflictDetection = ZeroJsonConfiguration.Default.discriminatorConflict
    )
    private val baseSerializer = PolymorphicSerializer(Any::class)
    private val baseDescriptor = descriptorCache.getOrCreate(baseSerializer.descriptor)
    private val json = TestZeroJson { serializersModule = this@PolymorphicSerializerCacheTest.serializersModule }

    @Test
    fun default_serializer_lookup() {
        val actual = cache.lookupDefaultDeserializer(baseSerializer, baseDescriptor)
        assertNotNull(actual)
        assertEquals(descriptorCache.getOrCreate(serialDescriptor<Long>()), actual.descriptor)
        assertEquals(kotlinx.serialization.serializer<Long>(), actual.deserializer)
    }

    @Test
    fun sub_class_serializer_lookup_1() {
        JsonContext.getThreadLocalPool(json).use { ctx ->
            val actual = cache.lookup(ctx.decoder, baseSerializer, baseDescriptor, "kotlin.Int".substringWrapper())
            assertNotNull(actual)
            assertEquals(kotlinx.serialization.serializer<Int>(), actual.deserializer)
            assertEquals(descriptorCache.getOrCreate(serialDescriptor<Int>()), actual.descriptor)
        }
    }

    @Test
    fun sub_class_serializer_lookup_2() {
        JsonContext.getThreadLocalPool(json).use { ctx ->
            val actual = cache.lookup(ctx.decoder, baseSerializer, baseDescriptor, "dev.dokky.zerojson.SimpleValueInteger".substringWrapper())
            assertNotNull(actual)
            assertEquals(SimpleValueInteger.serializer(), actual.deserializer)
            assertEquals(descriptorCache.getOrCreate(serialDescriptor<Int>()), actual.descriptor)
        }
    }
}