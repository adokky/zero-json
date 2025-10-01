package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonEncoderImpl
import dev.dokky.zerojson.internal.ZeroJsonBaseDecoder
import dev.dokky.zerojson.internal.nameForErrorMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

sealed class AbstractJsonElementSerializer<T: JsonElement>: KSerializer<T> {
    final override fun serialize(encoder: Encoder, value: T) {
        encoder.asZeroJsonEncoder().encodeJsonElement(value)
    }
}

object JsonElementSerializer: AbstractJsonElementSerializer<JsonElement>() {
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        "kotlinx.serialization.json.JsonElement",
        SerialKind.CONTEXTUAL
    )

    override fun deserialize(decoder: Decoder): JsonElement =
        decoder.asZeroJsonDecoder().decodeJsonElement()
}

object JsonObjectSerializer: AbstractJsonElementSerializer<JsonObject>() {

    private object JsonObjectDescriptor : SerialDescriptor by MapSerializer(String.serializer(), JsonElementSerializer).descriptor {
        @ExperimentalSerializationApi
        override val serialName: String = "kotlinx.serialization.json.JsonObject"
    }

    override val descriptor: SerialDescriptor = JsonObjectDescriptor

    override fun deserialize(decoder: Decoder): JsonObject =
        decoder.asZeroJsonDecoder().decodeJsonObject()
}

object JsonArraySerializer: AbstractJsonElementSerializer<JsonArray>() {
    private object JsonArrayDescriptor : SerialDescriptor by ListSerializer(JsonElementSerializer).descriptor {
        @ExperimentalSerializationApi
        override val serialName: String = "kotlinx.serialization.json.JsonArray"
    }

    override val descriptor: SerialDescriptor = JsonArrayDescriptor

    override fun deserialize(decoder: Decoder): JsonArray =
        decoder.asZeroJsonDecoder().decodeJsonArray()
}

object JsonPrimitiveSerializer: AbstractJsonElementSerializer<JsonPrimitive>() {
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.json.JsonPrimitive", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JsonPrimitive =
        decoder.asZeroJsonDecoder().decodeJsonPrimitive()
}

/**
 * Serializer object providing [kotlinx.serialization.SerializationStrategy] and [kotlinx.serialization.DeserializationStrategy] for [JsonNull].
 * It can only be used by with [ZeroJson] format and its input ([ZeroJsonDecoder] and [ZeroJsonEncoder]).
 */
object JsonNullSerializer : AbstractJsonElementSerializer<JsonNull>() {
    // technically, JsonNull is an object, but it does not call beginStructure/endStructure at all
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("kotlinx.serialization.json.JsonNull", SerialKind.CONTEXTUAL)

    override fun deserialize(decoder: Decoder): JsonNull {
        decoder.asZeroJsonDecoder()
        if (decoder.decodeNotNullMark()) {
            throw ZeroJsonDecodingException("Expected 'null' literal")
        }
        decoder.decodeNull()
        return JsonNull
    }
}

private fun Encoder.asZeroJsonEncoder(): JsonEncoderImpl =
    this as? JsonEncoderImpl ?: unexpectedEncoder(this)

private fun Decoder.asZeroJsonDecoder(): ZeroJsonBaseDecoder =
    this as? ZeroJsonBaseDecoder ?: unexpectedDecoder(this)

private fun unexpectedEncoder(encoder: Any): Nothing = throw IllegalStateException(
    "This serializer can only be used with ZeroJson format." +
    "Expected Encoder to be dev.dokky.zerojson.ZeroJsonEncoder, got ${encoder::class.nameForErrorMessage()}"
)

private fun unexpectedDecoder(decoder: Any): Nothing = throw IllegalStateException(
    "This serializer can only be used with ZeroJson format." +
    "Expected Decoder to be dev.dokky.zerojson.ZeroJsonDecoder, got ${decoder::class.nameForErrorMessage()}"
)