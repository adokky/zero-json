package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModuleCollector
import kotlin.reflect.KClass

internal class JsonSerializersModuleValidator(
    private val configuration: ZeroJsonConfiguration,
    private val cache: DescriptorCache
) : SerializersModuleCollector {
    override fun <T : Any> contextual(
        kClass: KClass<T>,
        provider: (typeArgumentsSerializers: List<KSerializer<*>>) -> KSerializer<*>
    ) {}

    override fun <Base : Any, Sub : Base> polymorphic(
        baseClass: KClass<Base>,
        actualClass: KClass<Sub>,
        actualSerializer: KSerializer<Sub>
    ) {
        val descriptor = actualSerializer.descriptor
        // This check is not necessary, because ZeroJson can automatically
        // wrap no-JSON-object polymorphic values into type/value wrapper.
//        checkKind(descriptor, actualClass)
        checkDiscriminatorCollisions(descriptor, actualClass)
    }

    // Copied from kotlinx.serialization.
//    private fun checkKind(descriptor: SerialDescriptor, actualClass: KClass<*>) {
//        val kind = descriptor.kind
//        if (kind is PolymorphicKind || kind == SerialKind.CONTEXTUAL) {
//            throw IllegalArgumentException(
//                "Serializer for ${actualClass.simpleName} can't be registered as a subclass for polymorphic serialization " +
//                "because its kind $kind is not concrete. To work with multiple hierarchies, register it as a base class."
//            )
//        }
//    }

    private fun checkDiscriminatorCollisions(descriptor: SerialDescriptor, actualClass: KClass<*>) {
        val elementInfo = cache.getOrCreate(descriptor).getElementInfoByName(configuration.classDiscriminator)
        if (!elementInfo.isUnknown) {
            throw IllegalArgumentException(
                "Polymorphic serializer for $actualClass has property '${configuration.classDiscriminator}' that conflicts " +
                "with JSON class discriminator. You can either change class discriminator in JsonConfiguration, " +
                "rename property with @SerialName annotation or fall back to array polymorphism"
            )
        }
    }

    override fun <Base : Any> polymorphicDefaultSerializer(
        baseClass: KClass<Base>,
        defaultSerializerProvider: (value: Base) -> SerializationStrategy<Base>?
    ) {}

    override fun <Base : Any> polymorphicDefaultDeserializer(
        baseClass: KClass<Base>,
        defaultDeserializerProvider: (className: String?) -> DeserializationStrategy<Base>?
    ) {}
}
