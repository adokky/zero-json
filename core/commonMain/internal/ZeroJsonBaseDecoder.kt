package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonDecoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal sealed interface ZeroJsonBaseDecoder: ZeroJsonDecoder {
    fun decodeJsonObject(): JsonObject
    fun decodeJsonArray(): JsonArray
    fun decodeJsonPrimitive(): JsonPrimitive
}