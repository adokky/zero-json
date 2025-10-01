package dev.dokky.zerojson.internal

import dev.dokky.zerojson.DiscriminatorConflictDetection
import io.kodec.text.AbstractSubString
import karamel.utils.unsafeCast
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlin.reflect.KClass

private typealias PolymorphicDeserializerProvider<Base> = (className: String?) -> DeserializationStrategy<Base>?
private typealias PolymorphicSerializerProvider<Base> = (value: Base) -> SerializationStrategy<Base>?

internal class PolymorphicSerializerCache(
    private val serializersModule: SerializersModule,
    private val descriptorCache: DescriptorCache,
    private val stableDefaultProviders: Boolean,
    private val discriminatorConflictDetection: DiscriminatorConflictDetection
) {
    private val polyBase2Serializers = HashMap<KClass<*>, KlassSerializers>()
    private val stringCache = HashMap<AbstractSubString, String>()

    private val tempDeserializerResult = DeserializerLookupResult(Unit.serializer(), ZeroJsonDescriptor.NOP, false)
    private val tempSerializerResult = SerializerLookupResult(ZeroJsonDescriptor.NOP, Unit.serializer(), ZeroJsonDescriptor.NOP, false)

    class KlassSerializers {
        var defaultDeserializerProvider: PolymorphicDeserializerProvider<*>? = null
        var defaultSerializerProvider: PolymorphicSerializerProvider<*>? = null
        var baseDescriptor: ZeroJsonDescriptor? = null

        val byDiscriminator = HashMap<AbstractSubString, DeserializerLookupResult>(4)
        val byActualClass = HashMap<KClass<*>, SerializerLookupResult>(4)
    }

    abstract class LookupResult {
        abstract var descriptor: ZeroJsonDescriptor
        abstract var discriminatorPresent: Boolean
    }

    data class DeserializerLookupResult(
        var deserializer: DeserializationStrategy<*>,
        override var descriptor: ZeroJsonDescriptor,
        override var discriminatorPresent: Boolean
    ): LookupResult()

    data class SerializerLookupResult(
        var base: ZeroJsonDescriptor,
        var serializer: SerializationStrategy<*>,
        override var descriptor: ZeroJsonDescriptor,
        override var discriminatorPresent: Boolean
    ): LookupResult() {
        val serialName: String get() = serializer.descriptor.serialName
    }

    init {
        serializersModule.dumpTo(object : SerializersModuleCollector {
            override fun <T : Any> contextual(
                kClass: KClass<T>,
                provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
            ) {}

            override fun <Base : Any, Sub : Base> polymorphic(
                baseClass: KClass<Base>,
                actualClass: KClass<Sub>,
                actualSerializer: KSerializer<Sub>
            ) {}

            override fun <Base : Any> polymorphicDefaultSerializer(
                baseClass: KClass<Base>,
                defaultSerializerProvider: PolymorphicSerializerProvider<Base>
            ) {
                val entry = getOrCreateCacheEntry(baseClass)
                if (entry.defaultSerializerProvider != null) throw SerializationException(
                    "attempt to register multiple default serializer providers for $baseClass"
                )
                entry.defaultSerializerProvider = defaultSerializerProvider
            }

            override fun <Base : Any> polymorphicDefaultDeserializer(
                baseClass: KClass<Base>,
                defaultDeserializerProvider: PolymorphicDeserializerProvider<Base>
            ) {
                val entry = getOrCreateCacheEntry(baseClass)
                if (entry.defaultDeserializerProvider != null) throw SerializationException(
                    "attempt to register multiple default deserializer providers for $baseClass"
                )
                entry.defaultDeserializerProvider = defaultDeserializerProvider
            }
        })
    }

    fun lookup(
        decoder: CompositeDecoder,
        baseSerializer: AbstractPolymorphicSerializer<*>,
        baseDescriptor: ZeroJsonDescriptor,
        discriminatorValue: AbstractSubString
    ): DeserializerLookupResult? {
        debugAssert { baseDescriptor.kind is PolymorphicKind }
        val serializers = getOrCreateCacheEntry(baseSerializer, baseDescriptor)

        serializers.byDiscriminator[discriminatorValue]?.let { return it }

        return if (serializers.defaultDeserializerProvider == null || stableDefaultProviders) {
            findAndCacheActualDeserializer(decoder, serializers, baseSerializer, baseDescriptor, discriminatorValue)
                ?: serializers.findDefaultDeserializerOrFail(baseSerializer, baseDescriptor, discriminatorValue)
        } else {
            lookupDeserializerSlow(decoder, serializers, baseDescriptor, baseSerializer, discriminatorValue)
        }
    }

    fun lookup(
        encoder: Encoder,
        baseSerializer: AbstractPolymorphicSerializer<*>,
        value: Any
    ): SerializerLookupResult? {
        val serializers = getOrCreateCacheEntry(baseSerializer)
        val actualClass = value::class

        serializers.byActualClass[actualClass]?.let { return it }

        return if (serializers.defaultSerializerProvider == null || stableDefaultProviders) {
            findAndCacheActualSerializer(encoder, baseSerializer, serializers, actualClass, value)
                ?: serializers.findDefaultSerializerOrFail(baseSerializer, value)
        } else {
            lookupSerializerSlow(encoder, baseSerializer, serializers, value)
        }
    }

    private fun lookupSerializerSlow(
        encoder: Encoder,
        baseDeserializer: AbstractPolymorphicSerializer<*>,
        serializers: KlassSerializers,
        value: Any
    ): SerializerLookupResult? {
        val actualSerializer = baseDeserializer
            .unsafeCast<AbstractPolymorphicSerializer<Any>>()
            .findPolymorphicSerializerOrNull(encoder, value)
            ?: return null

        tempSerializerResult.init(serializers.getOrPutBaseDescriptor(baseDeserializer), baseDeserializer, actualSerializer)
        return tempSerializerResult
    }

    private fun lookupDeserializerSlow(
        decoder: CompositeDecoder,
        serializers: KlassSerializers,
        baseDescriptor: ZeroJsonDescriptor,
        baseDeserializer: AbstractPolymorphicSerializer<*>,
        discriminatorValue: AbstractSubString
    ): DeserializerLookupResult? {
        if (serializers.baseDescriptor == null) serializers.baseDescriptor = baseDescriptor

        val actualDeserializer = discriminatorValue.toStringCached { discriminator ->
            baseDeserializer.findPolymorphicSerializerOrNull(decoder, discriminator)
        } ?: return null

        tempDeserializerResult.init(baseDescriptor, baseDeserializer, actualDeserializer)
        return tempDeserializerResult
    }

    private fun findAndCacheActualSerializer(
        encoder: Encoder,
        baseDeserializer: AbstractPolymorphicSerializer<*>,
        serializers: KlassSerializers,
        actualClass: KClass<*>,
        value: Any
    ): SerializerLookupResult? {
        val actualSerializer = baseDeserializer
            .unsafeCast<AbstractPolymorphicSerializer<Any>>()
            .findPolymorphicSerializerOrNull(encoder, value)
            ?: return null
        val baseDescriptor = descriptorCache.getOrCreate(baseDeserializer.descriptor)
        val lookupResult = SerializerLookupResult(baseDescriptor, baseDeserializer, actualSerializer)
        serializers.byActualClass[actualClass] = lookupResult
        return lookupResult
    }

    private fun KlassSerializers.findDefaultDeserializerOrFail(
        baseSerializer: AbstractPolymorphicSerializer<*>,
        baseDescriptor: ZeroJsonDescriptor,
        discriminatorValue: AbstractSubString
    ): DeserializerLookupResult? {
        val provider = defaultDeserializerProvider ?: return null
        // default serializer can not be cached because of the spec
        return lookupDefaultDeserializer(baseDescriptor, provider, discriminatorValue)
            ?: throwUnknownSubTypeError(baseSerializer, discriminatorValue)
    }

    private fun KlassSerializers.findDefaultSerializerOrFail(
        baseSerializer: AbstractPolymorphicSerializer<*>,
        value: Any
    ): SerializerLookupResult? {
        val baseDescriptor = getOrPutBaseDescriptor(baseSerializer)
        val provider = defaultSerializerProvider.unsafeCast<PolymorphicSerializerProvider<Any>?>() ?: return null
        // default serializer can not be cached because of the spec
        val serializer = provider(value) ?: throwUnknownSubTypeError(baseSerializer, value::class.nameForErrorMessage())
        tempSerializerResult.init(baseDescriptor, baseSerializer, serializer)
        return tempSerializerResult
    }

    private fun findAndCacheActualDeserializer(
        decoder: CompositeDecoder,
        serializers: KlassSerializers,
        baseDeserializer: AbstractPolymorphicSerializer<*>,
        baseDescriptor: ZeroJsonDescriptor,
        discriminatorValue: AbstractSubString
    ): DeserializerLookupResult? {
        if (serializers.baseDescriptor == null) serializers.baseDescriptor = baseDescriptor
        val typeName = discriminatorValue.toString()
        val actualDeserializer = baseDeserializer.findPolymorphicSerializerOrNull(decoder, typeName) ?: return null
        val lookupResult = DeserializerLookupResult(baseDescriptor, baseDeserializer, actualDeserializer)
        serializers.byDiscriminator[discriminatorValue.copy()] = lookupResult
        return lookupResult
    }

    fun lookupDefaultDeserializer(
        baseSerializer: AbstractPolymorphicSerializer<*>,
        baseDescriptor: ZeroJsonDescriptor
    ): DeserializerLookupResult? {
        val provider = polyBase2Serializers[baseSerializer.baseClass]?.defaultDeserializerProvider ?: return null
        return lookupDefaultDeserializer(baseDescriptor, provider, discriminatorValue = null)
    }

    private fun lookupDefaultDeserializer(
        baseDescriptor: ZeroJsonDescriptor,
        provider: PolymorphicDeserializerProvider<*>,
        discriminatorValue: AbstractSubString?
    ): DeserializerLookupResult? {
        val actualDeserializer = discriminatorValue.toStringCached { discriminator ->
            provider(discriminator) ?: return null
        }
        tempDeserializerResult.init(baseDescriptor, baseSerializer = null, actualDeserializer)
        return tempDeserializerResult
    }

    private fun getOrCreateCacheEntry(
        baseDeserializer: AbstractPolymorphicSerializer<*>,
        baseDescriptor: ZeroJsonDescriptor? = null
    ): KlassSerializers =
        getOrCreateCacheEntry(baseDeserializer.baseClass, baseDescriptor)

    private fun getOrCreateCacheEntry(
        baseClass: KClass<*>,
        baseDescriptor: ZeroJsonDescriptor? = null
    ): KlassSerializers =
        polyBase2Serializers.getOrPut(baseClass) {
            KlassSerializers().also { it.baseDescriptor = baseDescriptor }
        }

    private fun KlassSerializers.getOrPutBaseDescriptor(baseSerializer: AbstractPolymorphicSerializer<*>): ZeroJsonDescriptor =
        baseDescriptor ?: (descriptorCache.getOrCreate(baseSerializer.descriptor).also { baseDescriptor = it })

    private fun DeserializerLookupResult.init(
        base: ZeroJsonDescriptor,
        baseSerializer: AbstractPolymorphicSerializer<*>?,
        actualDeserializer: DeserializationStrategy<*>
    ) {
        this.deserializer = actualDeserializer
        initActualDescriptor(this, baseSerializer, actualDeserializer.descriptor, base)
    }

    private fun SerializerLookupResult.init(
        base: ZeroJsonDescriptor,
        baseSerializer: AbstractPolymorphicSerializer<*>?,
        actualSerializer: SerializationStrategy<*>
    ) {
        this.base = base
        this.serializer = actualSerializer
        initActualDescriptor(this, baseSerializer, actualSerializer.descriptor, base)
    }

    private fun DeserializerLookupResult(
        base: ZeroJsonDescriptor,
        baseSerializer: AbstractPolymorphicSerializer<*>?,
        actualDeserializer: DeserializationStrategy<*>
    ) =
        DeserializerLookupResult(deserializer = actualDeserializer, ZeroJsonDescriptor.NOP, false).also {
            initActualDescriptor(it, baseSerializer, actualDeserializer.descriptor, base)
        }

    private fun SerializerLookupResult(
        base: ZeroJsonDescriptor,
        baseSerializer: AbstractPolymorphicSerializer<*>?,
        actualSerializer: SerializationStrategy<*>
    ) =
        SerializerLookupResult(base = base, serializer = actualSerializer, ZeroJsonDescriptor.NOP, false).also {
            initActualDescriptor(it, baseSerializer, actualSerializer.descriptor, base)
        }

    private fun initActualDescriptor(
        result: LookupResult,
        baseSerializer: AbstractPolymorphicSerializer<*>?,
        actualSerialDescriptor: SerialDescriptor,
        baseDescriptor: ZeroJsonDescriptor
    ) {
        val actualDescriptor = actualSerialDescriptor.getActualDescriptor()
        result.descriptor = actualDescriptor
        val closedPolymorphism = baseSerializer is SealedClassSerializer

        if (actualDescriptor.kind.let { it == StructureKind.CLASS || it == StructureKind.OBJECT }) {
            val discriminator = baseDescriptor.classDiscriminator!!
            val conflictedField = actualDescriptor.getElementInfoByName(discriminator)
            if (conflictedField.isValid && conflictedField != actualDescriptor.inlineMapElement) {
                if (discriminatorConflictDetection != DiscriminatorConflictDetection.DISABLED &&
                    (closedPolymorphism || discriminatorConflictDetection == DiscriminatorConflictDetection.ALL) &&
                    !baseDescriptor.allowMaterializedDiscriminator &&
                    !actualDescriptor.allowMaterializedDiscriminator
                ) {
                    discriminatorConflict(discriminator, actualDescriptor.serialName)
                }
                result.discriminatorPresent = true
            }
        }
    }

    private fun SerialDescriptor.getActualDescriptor(): ZeroJsonDescriptor =
        descriptorCache.getOrCreate(unnestInlineAndContextual(serializersModule))

    private fun discriminatorConflict(discriminator: String, actualSerialName: String): Nothing =
        throw SerializationException(
            "property '$discriminator' of class with serial name " +
            "'$actualSerialName' conflicts with discriminator"
        )

    private inline fun <R> AbstractSubString?.toStringCached(use: (String?) -> R): R {
        val cached: String?
        val asString: String?

        if (this == null) {
            cached = null
            asString = null
        } else {
            // avoid allocations by caching discriminator String
            cached = stringCache[this]
            asString = cached ?: this.toString()
        }

        val result = use(asString)

        // important: cache only after successful use
        if (cached == null && asString != null) {
            stringCache[this!!.copy()] = asString
        }

        return result
    }
}

@OptIn(InternalSerializationApi::class)
internal fun throwUnknownSubTypeError(baseSerializer: AbstractPolymorphicSerializer<*>, subType: Any): Nothing {
    throw SerializationException(
        "Serializer for subclass '$subType' is not found in the " +
        "polymorphic scope of '${baseSerializer.baseClass.nameForErrorMessage()}'"
    )
}

internal fun PolymorphicSerializerCache.LookupResult?.isDiscriminatorPresent() = this != null && this.discriminatorPresent