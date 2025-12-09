package kotlinx.serialization.test

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.ByteArrayOutputStream

fun <T> Json.encodeViaStream(
    serializer: SerializationStrategy<T>,
    value: T
): String {
    val output = ByteArrayOutputStream()
    encodeToStream(serializer, value, output)
    return output.toString(Charsets.UTF_8.name())
}

fun <T> Json.decodeViaStream(
    serializer: DeserializationStrategy<T>,
    input: String
): T = decodeFromStream(serializer, input.byteInputStream())
