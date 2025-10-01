package dev.dokky.zerojson

import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder

interface ZeroJsonEncoder: Encoder, JsonEncoder {
    val zeroJson: ZeroJson

    val writer: JsonWriter

    /**
     * Appends the given JSON [element] to the current output.
     * This method is allowed to invoke only as the part of the whole serialization process of the class,
     * calling this method after invoking [beginStructure] or any `encode*` method will lead to unspecified behaviour
     * and may produce an invalid JSON result.
     * For example:
     * ```
     * class Holder(val value: Int, val list: List<Int>())
     *
     * // Holder serialize method
     * fun serialize(encoder: Encoder, value: Holder) {
     *     // Completely okay, the whole Holder object is read
     *     val jsonObject = JsonObject(...) // build a JsonObject from Holder
     *     (encoder as ZeroJsonEncoder).encodeJsonElement(jsonObject) // Write it
     * }
     *
     * // Incorrect Holder serialize method
     * fun serialize(encoder: Encoder, value: Holder) {
     *     val composite = encoder.beginStructure(descriptor)
     *     composite.encodeSerializableElement(descriptor, 0, Int.serializer(), value.value)
     *     val array = JsonArray(value.list)
     *     // Incorrect, encoder is already in an intermediate state after encodeSerializableElement
     *     (composite as ZeroJsonEncoder).encodeJsonElement(array)
     *     composite.endStructure(descriptor)
     *     // ...
     * }
     * ```
     */
    override fun encodeJsonElement(element: JsonElement)
}
