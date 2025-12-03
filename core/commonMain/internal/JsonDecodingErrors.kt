package dev.dokky.zerojson.internal

import dev.dokky.zerojson.StructuredMapKeysMode
import dev.dokky.zerojson.ZeroJsonConfiguration
import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.text.TextReader
import karamel.utils.readableName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlin.reflect.KClass

/*
TODO clean up
ErrorsCommon.kt
ErrorsDecoding.kt
ErrorsEncoding.kt

Most decoding errors should come in two forms: debug and fast/safe to expose.
Some errors should have unique exception class fot distinguishing.
*/

internal fun unknownKeyError(key: Any, position: Int = -1): Nothing {
    throw ZeroJsonDecodingException(
        message = "Encountered an unknown key '$key'. " +
                "Use 'ignoreUnknownKeys = true' in 'ZeroJson {}' builder or '@JsonIgnoreUnknownKeys' " +
                "annotation to ignore unknown keys.",
        position = position
    )
}

// TODO MissingFieldException is more appropriate?
internal fun missingDiscriminator(
    baseDeserializer: AbstractPolymorphicSerializer<*>,
    position: Int = -1
): Nothing {
    throw ZeroJsonDecodingException(
        message = "Class discriminator was missing and no default serializers were registered in " +
                "the polymorphic scope of '${baseDeserializer.baseClass.nameForErrorMessage()}'",
        position = position
    )
}

internal fun throwSubtypeNotRegistered(subClassName: String?, baseClass: KClass<*>): Nothing {
    val baseClassName = baseClass.nameForErrorMessage()
    throw SerializationException(
        if (subClassName == null) {
            "Class discriminator was missing and no default serializers were " +
            "registered in the polymorphic scope of '$baseClassName'."
        } else {
            "Serializer for subclass '$subClassName' is not found in the polymorphic scope of '$baseClassName'.\n" +
            "Check if class with serial name '$subClassName' exists and serializer is " +
            "registered in a corresponding SerializersModule.\n" +
            "To be registered automatically, class '$subClassName' has to be '@Serializable', " +
            "and the base class '$baseClassName' has to be sealed and '@Serializable'."
        }
    )
}

internal fun KClass<*>.nameForErrorMessage(): String {
    var name = readableName()
    if (name.startsWith("kotlin.")) name = name.substringAfterLast('.')
    return name
}

internal fun unknownEnumEntry(entryName: Any, position: Int = -1): Nothing =
    throw ZeroJsonDecodingException(
        message = "An unknown entry '$entryName'",
        position = position
    )

internal fun TextReader.throwExpectedString(): Nothing = fail("expected string")

internal fun TextReader.throwExpectedStringGotNull(): Nothing = fail("expected string, got null")

internal fun TextReader.throwExpectedStringGotBool(): Nothing = fail("expected string, got boolean")

internal fun TextReader.throwExpectedJsonElement(): Nothing = fail("expected JSON element")

internal fun throwExpectedKotlinxEndec(decoder: Boolean): Nothing {
    val type = if (decoder) "Decoder" else "Encoder"
    error(
        "Serializer expected $type to be kotlinx.serialization.Json$type. " +
        "Use 'zero-json-kotlinx' artifact (JVM only) or " +
        "update the serializer to use ZeroJson API counterpart"
    )
}

private fun throwInvalidKeyKind(keyDescriptor: SerialDescriptor): Nothing = throw SerializationException(
    "Value of type '${keyDescriptor.serialName}' can't be used in JSON as a key in the map. " +
    "It should have either primitive or enum kind, but its kind is '${keyDescriptor.kind}'.\n" +
    "Use 'allowStructuredMapKeys = true' in 'ZeroJson {}'"
)

internal fun ZeroJsonConfiguration.throwIfStructuredKeysDisabled(keyDescriptor: SerialDescriptor) {
    if (structuredMapKeysMode == StructuredMapKeysMode.DISABLED) {
        throwInvalidKeyKind(keyDescriptor)
    }
}

internal fun throwNansAreNotAllowed(num: Number, position: Int = -1): Nothing {
    val message = "Unexpected special floating-point value $num. By default, " +
            "non-finite floating point values are prohibited because they do not conform JSON specification"
    throw when {
        position < 0 -> SerializationException(message)
        else -> ZeroJsonDecodingException(message, position = position)
    }
}

internal fun throwInlineMapCompositeKeysAreNotSupported(): Nothing = throw SerializationException(
    "attempt to decode inline map with structured key (structuredMapKeysMode = LIST)"
)

internal fun checkIfMapIsMissingValue(prevIdx: Int) {
    if (prevIdx and 1 == 0) {
        throw ZeroJsonDecodingException("the list should consist of key-value pairs following each other")
    }
}