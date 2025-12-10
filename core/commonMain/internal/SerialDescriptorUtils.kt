package dev.dokky.zerojson.internal

import dev.dokky.zerojson.StructuredMapKeysMode
import dev.dokky.zerojson.ZeroJsonConfiguration
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.SerializersModule

internal fun SerialDescriptor.isDeepNullable(): Boolean {
    var desc = this
    while (true) {
        if (desc.isNullable) return true
        if (desc.isInline) desc = desc.getElementDescriptor(0) else break
    }
    return false
}

internal fun SerialDescriptor.unnestInline(): SerialDescriptor {
    var result = this
    var nullable = isNullable
    while (result.isInline) {
        result = result.getElementDescriptor(0)
        nullable = nullable || result.isNullable
    }
    return if (nullable) result.nullable else result
}

internal fun SerialDescriptor.unnestInlineAndContextual(serializersModule: SerializersModule): SerialDescriptor {
    var result = this
    var nullable = isNullable

    var prev = result
    while (true) {
        if (result.isInline) {
            result = result.getElementDescriptor(0)
            nullable = nullable || result.isNullable
        }
        if (result.kind == SerialKind.CONTEXTUAL) {
            result = serializersModule.getContextualDescriptor(result) ?: serializerNotFound(result)
            nullable = nullable || result.isNullable
        }
        if (result === prev) break
        prev = result
    }

    return if (nullable) result.nullable else result
}

private fun serializerNotFound(result: SerialDescriptor): Nothing = throw SerializationException(
    "Serializer for '${result.capturedKClass?.simpleName ?: result.serialName}' is not found.\n" +
    "Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.\n"
)

internal fun SerialDescriptor.asNullableEnum(): SerialDescriptor? {
    var desc = this
    var nullable = false
    while (true) {
        if (desc.isNullable) nullable = true
        if (nullable && desc.kind == SerialKind.ENUM) return desc
        if (desc.isInline) desc = desc.getElementDescriptor(0) else return null
    }
}

internal inline fun <reified T: Annotation> SerialDescriptor.findElementAnnotation(index: Int): T? =
    getElementAnnotations(index).filterIsInstance<T>().firstOrNull()

internal inline fun <reified T: Annotation> SerialDescriptor.hasElementAnnotation(index: Int): Boolean =
    findElementAnnotation<T>(index) != null

internal fun SerialDescriptor.isMapWithStructuredKey(config: ZeroJsonConfiguration): Boolean =
    config.structuredMapKeysMode == StructuredMapKeysMode.LIST &&
    getElementDescriptor(0).unnestInline().kind.getFlags().isCompound