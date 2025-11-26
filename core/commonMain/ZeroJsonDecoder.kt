package dev.dokky.zerojson

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.ChunkedDecoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

// todo add ZeroJsonEncoder/Decoder.defer(closable: AutoClosable)
interface ZeroJsonDecoder: JsonDecoder {
    val zeroJson: ZeroJson

    /**
     * Decodes the next element in the current input as [JsonElement].
     * The type of the decoded element depends on the current state of the input and, when received
     * by [serializer][KSerializer] in its [KSerializer.serialize] method, the type of the token directly matches
     * the [kind][SerialDescriptor.kind].
     *
     * This method is allowed to invoke only as the part of the whole deserialization process of the class,
     * calling this method after invoking [beginStructure] or any `decode*` method will lead to unspecified behaviour.
     * For example:
     * ```
     * class Holder(val value: Int, val list: List<Int>())
     *
     * // Holder deserialize method
     * fun deserialize(decoder: Decoder): Holder {
     *     // Completely okay, the whole Holder object is read
     *     val jsonObject = (decoder as ZeroJsonDecoder).decodeJsonElement()
     *     // ...
     * }
     *
     * // Incorrect Holder deserialize method
     * fun deserialize(decoder: Decoder): Holder {
     *     // decode "value" key unconditionally
     *     decoder.decodeElementIndex(descriptor)
     *     val value = decode.decodeInt()
     *     // Incorrect, decoder is already in an intermediate state after decodeInt
     *     val json = (decoder as ZeroJsonDecoder).decodeJsonElement()
     *     // ...
     * }
     * ```
     */
    override fun decodeJsonElement(): JsonElement
}


interface ZeroJsonTextDecoder: ZeroJsonDecoder, ChunkedDecoder {
    val reader: JsonReader
}