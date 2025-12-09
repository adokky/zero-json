package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonContext
import kotlinx.serialization.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializes the [value] with [serializer] into a [stream] using JSON format and UTF-8 encoding.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream cannot be written to.
 */
@ExperimentalSerializationApi
fun <T> ZeroJson.encodeToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream
) {
    stream.write(encodeToByteArray(serializer, value))
}

/**
 * Serializes given [value] to [stream] using UTF-8 encoding and serializer retrieved from the reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 * @throws [IOException] If an I/O error occurs and stream cannot be written to.
 */
@ExperimentalSerializationApi
inline fun <reified T> ZeroJson.encodeToStream(
    value: T,
    stream: OutputStream
): Unit =
    encodeToStream(serializersModule.serializer(), value, stream)

/**
 * Deserializes JSON from [stream] using UTF-8 encoding to a value of type [T] using [deserializer].
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 * @throws [IOException] If an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
fun <T> ZeroJson.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream
): T {
    return JsonContext.useThreadLocal(this) {
        val buf = tempBuffer()
        val array = buf.array
        val bytesRead = stream.read(array)
        if (bytesRead == array.size) {
            throw SerializationException("reached maximum input size ${config.maxEncodedBytes} bytes")
        }
        if (bytesRead <= 0) {
            throw SerializationException("empty input")
        }
        buf.setArray(array, 0, bytesRead)
        try {
            decode(deserializer, buf)
        } finally {
            buf.setArray(array, 0, array.size)
        }
    }
}

/**
 * Deserializes the contents of given [stream] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * Note that this functions expects that exactly one object would be present in the stream
 * and throws an exception if there are any dangling bytes after an object.
 *
 * @throws [SerializationException] if the given JSON input cannot be deserialized to the value of type [T].
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 * @throws [IOException] If an I/O error occurs and stream cannot be read from.
 */
@ExperimentalSerializationApi
inline fun <reified T> ZeroJson.decodeFromStream(stream: InputStream): T =
    decodeFromStream(serializersModule.serializer(), stream)