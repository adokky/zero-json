package dev.dokky.zerojson.internal

import kotlinx.serialization.descriptors.*

internal class DescriptorCache(internal val config: DescriptorCacheConfig) {
    // TODO perf: single ConcurrentHashMap instead of many thread-local caches
    private val map = HashMap<SerialDescriptor, ZeroJsonDescriptor>(256)

    fun getOrCreate(descriptor: SerialDescriptor): ZeroJsonDescriptor =
        getPredefinedDescriptor(descriptor) ?: getOrCreateUnsafe(descriptor)

    fun getOrCreateUnsafe(descriptor: SerialDescriptor): ZeroJsonDescriptor =
        map[descriptor] ?: createAndRegister(descriptor)

    private fun createAndRegister(descriptor: SerialDescriptor): ZeroJsonDescriptor {
        if (descriptor.isNullable) {
            // Reuse descriptor data by making cheap nullable non-deep copy.
            // fixme can create unnecessary copies on recursive structures
            val nonNullable = getOrCreate(descriptor.nonNullOriginal)
            val result = nonNullable.copyAsNullable(descriptor)
            map[descriptor] = result
            return result
        }

        // Using temporary hash-map to make atomic changes to main registry hash-map.
        // Allows concurrent writes to main registry hash-map.
        val tempRegistry = HashMap<SerialDescriptor, ZeroJsonDescriptor>()
        val result = create(descriptor, tempRegistry)
        map.putAll(tempRegistry)

        return result
    }

    private fun create(
        descriptor: SerialDescriptor,
        registry: HashMap<SerialDescriptor, ZeroJsonDescriptor>
    ): ZeroJsonDescriptor {
        if (descriptor.kind == SerialKind.CONTEXTUAL) {
            config.serializersModule.getContextualDescriptor(descriptor)?.let { actualDesc ->
                if (actualDesc === descriptor) error("contextual serializer pointed to itself")
                val result = getOrCreateUnsafe(actualDesc)
                registry[descriptor] = result
                return result
            }
        }

        val result = ZeroJsonDescriptor(descriptor, config)

        // cyclic referencing protection: store uninitialized result upfront
        registry[descriptor] = result

        var elements: Array<ZeroJsonDescriptor?> = emptyArray()
        if (descriptor.elementsCount > 0 && descriptor.kind.isClassLike()) {
            elements = Array(descriptor.elementsCount) { i ->
                val elementDesc = descriptor.getElementDescriptor(i).unnestInline()
                when {
                    elementDesc.kind == SerialKind.CONTEXTUAL -> null
                    else -> {
                        getPredefinedDescriptor(elementDesc)
                            ?: map[elementDesc]
                            ?: registry[elementDesc]
                            ?: create(elementDesc, registry)
                    }
                }
            }
        }

        result.initElements(elements, config.useAlternativeNames, config.namingStrategy)
        return result
    }

    private fun getPredefinedDescriptor(descriptor: SerialDescriptor): ZeroJsonDescriptor? =
        when(descriptor.kind) {
            is StructureKind.MAP -> ZeroJsonDescriptor.MAP
            is StructureKind.LIST -> ZeroJsonDescriptor.LIST
            else -> null
        }
}