package dev.dokky.zerojson.internal

import dev.dokky.pool.SimpleObjectPool
import dev.dokky.pool.use
import dev.dokky.zerojson.JsonReaderConfig
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.Buffer
import io.kodec.buffers.MutableBuffer
import io.kodec.buffers.emptyByteArray
import io.kodec.text.*
import karamel.utils.buildString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.modules.EmptySerializersModule
import kotlin.jvm.JvmStatic

internal class JsonContext(
    val ownerPool: SimpleObjectPool<JsonContext>,
    val zeroJson: ZeroJson,
    json: Json?,
    val stringBuilder: StringBuilder,
    val messageBuilder: StringBuilder,
    val descriptorCache: DescriptorCache,
    val polymorphicDeserializerResolver: PolymorphicSerializerCache
) {
    val config = zeroJson.configuration

    init {
        if (config.serializersModule !== EmptySerializersModule()) {
            config.serializersModule.dumpTo(JsonSerializersModuleValidator(config, descriptorCache))
        }
    }

    val bufferInput = ZeroUtf8TextReader(messageBuilder)
    val stringInput = ZeroStringTextReader(messageBuilder)

    val bufferWriter = BufferTextWriter()
    val stringWriter = StringTextWriter(stringBuilder)

    private val stringWriterClosable = AutoCloseable { stringWriter.output = stringBuilder }
    private val bufferWriterClosable = AutoCloseable { bufferWriter.set(ArrayBuffer.Empty) }

    val tempArrayWrapper = ArrayBuffer(0)
    var tempBuffer: ArrayBuffer? = null

    private val jsonTextWriter = JsonTextWriter(bufferWriter,
        allowNaNs = config.allowSpecialFloatingPointValues)

    private val jsonTreeWriter = JsonTreeWriter(stringBuilder,
        allowNaNs = config.allowSpecialFloatingPointValues,
        maxDepth = config.maxStructureDepth)
    
    var jsonWriter: JsonWriterBase = jsonTextWriter
        private set

    val reader = JsonReaderImpl(bufferInput,
        JsonReaderConfig(
            expectStringQuotes = !config.isLenient,
            allowComments = config.allowComments,
            allowSpecialFloatingPointValues = config.allowSpecialFloatingPointValues,
            stringBuilder = stringBuilder,
            messageBuilder = messageBuilder,
            depthLimit = config.maxStructureDepth,
            allowTrailingComma = config.allowTrailingComma
        )
    )

    val textDecodingStack = JsonTextDecodingStack(
        capacity = config.maxStructureDepth,
        maxInlinedElements = config.maxInlineProperties
    )
    val treeDecodingStack = JsonTreeDecodingStack(config)

    // this-referencing structures must come last

    val decoder = JsonTextDecoder(this, reader, json)

    val encoderStack: AutoCloseableStack<JsonEncoderImpl> = AutoCloseableStack(config,
        first = JsonEncoderImpl(this, parent = null, json),
        create = { parent -> JsonEncoderImpl(this, parent = parent, json) })

    val treeDecoderStack: AutoCloseableStack<JsonTreeDecoder> = AutoCloseableStack(config,
        first = JsonTreeDecoder(this, parent = null, json),
        create = { parent -> JsonTreeDecoder(this, parent = parent, json) })

    fun nextTreeDecoder(
        descriptor: ZeroJsonDescriptor,
        parentElement: JsonElement,
    ): JsonTreeDecoder = treeDecoderStack.next {
        this.parentElement = parentElement
        this.descriptor = descriptor
    }

    fun releaseTreeDecoder(decoder: JsonTreeDecoder) {
        debugAssert {
            (treeDecoderStack.acquired == 1) == (decoder.descriptor === ZeroJsonDescriptor.ROOT)
        }
        treeDecoderStack.release(decoder)
    }

    fun nextEncoder(
        zDescriptor: ZeroJsonDescriptor,
        inlineRootEncoder: JsonEncoderImpl?,
        jsonInlined: Boolean,
        elementIndex: Int,
        insideMapKey: Boolean
    ): JsonEncoderImpl = encoderStack.next {
        init(zDescriptor,
            inlineRootEncoder = inlineRootEncoder ?: this,
            jsonInlined = jsonInlined,
            elementIndex = elementIndex,
            discriminatorKey = null,
            discriminatorValue = null,
            insideMapKey = insideMapKey,
            discriminatorPresent = false
        )
    }

    fun nextPolymorphicSubEncoder(
        zDescriptor: ZeroJsonDescriptor,
        discriminatorKey: String,
        discriminatorValue: String,
        discriminatorPresent: Boolean
    ): JsonEncoderImpl = encoderStack.next {
        init(zDescriptor,
            inlineRootEncoder = this, // polymorphic objects can not be inlined
            jsonInlined = false,
            elementIndex = 0,
            discriminatorKey = discriminatorKey,
            discriminatorValue = discriminatorValue,
            discriminatorPresent = discriminatorPresent,
            insideMapKey = false // will be updated on demand by decoder itself
        )
    }

    fun releaseEncoder(encoder: JsonEncoderImpl) {
        encoderStack.release(encoder)
    }

    fun <T> encode(value: T, serializationStrategy: SerializationStrategy<T>, output: StringBuilder) {
        stringWriter.output = output
        encode(serializationStrategy, value, stringWriter, stringWriterClosable) {}
    }

    fun <T> encode(value: T, serializationStrategy: SerializationStrategy<T>, output: MutableBuffer, offset: Int = 0): Int {
        bufferWriter.set(output, offset)
        return encode(serializationStrategy, value, bufferWriter, bufferWriterClosable) {
            bufferWriter.position
        }
    }

    fun <T> encodeToJsonElement(serializationStrategy: SerializationStrategy<T>, value: T): JsonElement {
        jsonWriter = jsonTreeWriter
        jsonTreeWriter.beginEncoding()
        return encode(serializationStrategy, value, jsonTreeWriter) {
            jsonTreeWriter.endEncoding()
        }
    }

    private fun <T, R> encode(
        serializationStrategy: SerializationStrategy<T>,
        value: T,
        output: TextWriter,
        closable: AutoCloseable?,
        result: () -> R
    ): R {
        jsonWriter = jsonTextWriter
        jsonTextWriter.textWriter = output
        return encode(serializationStrategy, value, closable, result)
    }

    private inline fun <T, R> encode(
        serializationStrategy: SerializationStrategy<T>,
        value: T,
        closable: AutoCloseable?,
        result: () -> R
    ): R {
        try {
            nextEncoder(
                descriptorCache.getOrCreate(serializationStrategy.descriptor),
                inlineRootEncoder = null,
                jsonInlined = false,
                elementIndex = -1,
                insideMapKey = false
            ).encodeSerializableValue(serializationStrategy, value)
            return result()
        } finally {
            encoderStack.close()
            closable?.close()
        }
    }

    fun <T> decode(deserializationStrategy: DeserializationStrategy<T>, input: Buffer, offset: Int = 0): T {
        bufferInput.startReadingFrom(input, offset)
        return decode(deserializationStrategy, bufferInput)
    }

    fun beginReadFrom(input: RandomAccessTextReader): JsonTextDecoder {
        reader.input = input
        textDecodingStack.clear()
        textDecodingStack.enter(ZeroJsonDescriptor.ROOT)
        decoder.startReading()
        return decoder
    }

    fun endRead() {
        reader.input = bufferInput
    }

    private fun <T> decode(
        deserializationStrategy: DeserializationStrategy<T>,
        input: RandomAccessTextReader
    ): T {
        reader.input = input
        textDecodingStack.clear()
        textDecodingStack.enter(ZeroJsonDescriptor.ROOT)
        try {
            decoder.startReading()
            return decoder.decodeSerializableValue(deserializationStrategy).also {
                input.expectEof()
            }
        } catch (e: TextDecodingException) {
            throw ZeroJsonDecodingException(
                e.message ?: "",
                position = if (e.position < 0) decoder.reader.position else e.position,
                path = textDecoderJsonPath(),
                cause = e.takeIf { config.fullStackTraces && ZeroJson.captureStackTraces }
            )
        } catch (e: ZeroJsonDecodingException) {
            if (e.position < 0) e.position = decoder.reader.position
            if (e.path == null) e.path = textDecoderJsonPath()
            throw e
        } catch (e: MissingFieldException) {
            rethrowWithPath(e, textDecoderJsonPath())
        }
        finally {
            input.resetInput()
            decoder.close()
        }
    }

    private fun rethrowWithPath(e: MissingFieldException, path: String): Nothing {
        val msg = buildString(messageBuilder) {
            if (e.message != null) {
                append(e.message)
                append(" at ")
            }
            append(path)
        }
        throw MissingFieldException(e.missingFields, message = msg, cause = null)
    }

    fun <T> decode(deserializationStrategy: DeserializationStrategy<T>, input: CharSequence, offset: Int = 0): T {
        stringInput.startReadingFrom(input, offset)
        return decode(deserializationStrategy, stringInput)
    }

    fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        stringBuilder.setLength(0)
        encode(value, serializer, stringBuilder)
        return stringBuilder.toString()
    }

    fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val buf = tempBuffer ?: (ArrayBuffer(config.maxOutputBytes).also { tempBuffer = it })
        val written = encode(value, serializer, buf)
        return buf.toByteArray(endExclusive = written)
    }

    fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray, offset: Int): T {
        tempArrayWrapper.setArray(bytes, start = offset)
        try {
            return decode(deserializer, tempArrayWrapper)
        } finally {
            tempArrayWrapper.setArray(emptyByteArray)
        }
    }

    fun <T> decodeFromJsonElement(deserializer: DeserializationStrategy<T>, element: JsonElement): T {
        val rootTreeDecoder = nextTreeDecoder(ZeroJsonDescriptor.ROOT, parentElement = JsonNull)
        rootTreeDecoder.element = element
        try {
            return rootTreeDecoder.decodeSerializableValue(deserializer)
        } catch (e: ZeroJsonDecodingException) {
            if (e.path == null) e.path = treeDecoderJsonPath()
            throw e
            // todo global variable to control stack traces
//        } catch (e: NumberFormatException) {
//            val element = (treeDecoderStack.get(treeDecoderStack.acquired - 1).element as? JsonPrimitive)?.content ?: ""
//            throw ZeroJsonDecodingException("invalid number format: '${element}'",
//                path = treeDecoderJsonPath(),
//                cause = e.takeIf { config.fullStackTraces && ZeroJson.captureStackTraces }
//            )
        } catch (e: MissingFieldException) {
            rethrowWithPath(e, treeDecoderJsonPath())
        } finally {
            treeDecodingStack.clear()
            treeDecoderStack.close()
        }
    }

    private fun textDecoderJsonPath(): String = buildString(stringBuilder) {
        textDecodingStack.currentJsonPath(reader, this)
    }

    private fun treeDecoderJsonPath(): String = buildString(stringBuilder) { treeDecoderJsonPath(this) }

    private fun treeDecoderJsonPath(output: StringBuilder) = output.run {
        append('$')
        for (i in 1 until treeDecoderStack.acquired) {
            val decoder = treeDecoderStack.get(i)
            if (decoder.elementIndex < 0) break

            when(decoder.descriptor.kind) {
                StructureKind.MAP -> decoder.currentKey?.let { appendSegment(it) } ?: break
                StructureKind.LIST -> append('[').append(decoder.elementIndex).append(']')
                StructureKind.CLASS -> {
                    if (decoder.elementIndex >= decoder.descriptor.elementsCount) break
                    if (decoder.descriptor.isElementJsonInline(decoder.elementIndex)) continue
                    val key = decoder.currentKey
                    if (key == null) append("[null]") else appendSegment(key)
                }
                else -> break
            }
        }
    }

    private fun StringBuilder.appendSegment(key: String) {
        if (key.jsonPathSegmentRequireEscaping(segmentStart = 0)) {
            append("['")
            appendJsonPathSegment(key, segmentStart = 0, segmentEnd = key.length)
            append("']")
        } else {
            append('.').append(key)
        }
    }

    companion object {
        inline fun <R> useThreadLocal(json: ZeroJson, body: JsonContext.() -> R): R =
            getThreadLocalPool(json).use(body)

        @JvmStatic
        fun getThreadLocalPool(json: ZeroJson): SimpleObjectPool<JsonContext> =
            ThreadLocalCaches.getInstance().getOrCreateContextPool(json)
    }
}