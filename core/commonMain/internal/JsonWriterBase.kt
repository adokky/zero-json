package dev.dokky.zerojson.internal

import dev.dokky.zerojson.JsonWriter
import kotlinx.serialization.json.JsonObject

internal sealed class JsonWriterBase: JsonWriter {
    abstract fun write(
        element: JsonObject,
        discriminatorKey: String?,
        discriminatorValue: String?,
        skipNullKeys: Boolean
    )
}