package dev.dokky.zerojson.internal

import dev.dokky.zerojson.JsonNumberIsOutOfRange
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonTextDecoder
import io.kodec.text.IntegerParsingError
import io.kodec.text.readLong
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.ChunkedDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule

internal class JsonTextDecoderForUnsignedTypes(private val parentDecoder: JsonTextDecoder):
    AbstractDecoder(), ZeroJsonTextDecoder, ChunkedDecoder
{
    override val serializersModule: SerializersModule get() = parentDecoder.serializersModule
    override val reader: JsonReaderImpl get() = parentDecoder.reader
    override val zeroJson: ZeroJson get() = parentDecoder.zeroJson
    override val json: Json get() = parentDecoder.json

    override fun decodeJsonElement(): JsonElement = parentDecoder.decodeJsonElement()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = error("unsupported")

    override fun decodeByte(): Byte = reader.readUInt(8).toByte()

    override fun decodeShort(): Short = reader.readUInt(16).toShort()

    override fun decodeInt(): Int = reader.readUInt(32).toInt()

    override fun decodeLong(): Long = parentDecoder.reader.maybeQuoted {
        input.readJsonUnsingedLong().toLong()
    }

    override fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit) {
        parentDecoder.decodeStringChunked(consumeChunk)
    }
}

private val unsignedNumberDescriptors = setOf(
    UByte.serializer().descriptor,
    UShort.serializer().descriptor,
    UInt.serializer().descriptor,
    ULong.serializer().descriptor
)

internal val SerialDescriptor.isUnsignedNumber: Boolean
    get() = this.isInline && this in unsignedNumberDescriptors

internal fun JsonReaderImpl.readUInt(bits: Int, quotes: Boolean = true): Long {
    val start = position
    val error = input.errorContainer.prepare<IntegerParsingError>()
    val result = maybeQuoted(allowQuotes = quotes) {
        input.readLong(onFormatError = error)
    }
    var overflow = false
    error.consumeError { err ->
        when(err) {
            IntegerParsingError.MalformedNumber -> {
                position = start
                fail(error)
            }
            IntegerParsingError.Overflow -> overflow = true
        }
    }
    val max = (0L.inv() shl bits).inv()
    if (result and max.inv() != 0L || overflow) {
        position = start
        throw JsonNumberIsOutOfRange(0, max)
    }
    return result
}