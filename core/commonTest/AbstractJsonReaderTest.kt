package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.ZeroStringTextReader
import dev.dokky.zerojson.internal.ZeroTextReader
import dev.dokky.zerojson.internal.ZeroUtf8TextReader
import io.kodec.buffers.ArrayBuffer
import kotlinx.serialization.SerializationException
import kotlin.random.Random
import kotlin.test.assertEquals

abstract class AbstractJsonReaderTest {
    private val buffer = ArrayBuffer(0)
    internal val bufferInput = ZeroUtf8TextReader()
    internal val stringInput = ZeroStringTextReader()
    internal val reader = JsonReaderImpl(bufferInput, config = JsonReaderConfig())

    protected fun setBufferJson(json: String, offset: Int = Random.nextInt(7)) {
        buffer.setArray(ByteArray(offset) + json.encodeToByteArray())
        bufferInput.startReadingFrom(buffer, offset)
        reader.input = bufferInput
        reader.skipWhitespace()
    }

    protected fun setStringJson(
        json: String,
        offset: Int = Random.nextInt(7),
        skipWhitespace: Boolean = true
    ) {
        val prefix = (1..offset).joinToString("")
        stringInput.startReadingFrom(prefix + json, offset)
        reader.input = stringInput
        if (skipWhitespace) reader.skipWhitespace()
    }

    private inline fun enrichError(
        input: String,
        binary: Boolean,
        offset: Int,
        body: () -> Unit
    ) {
        try {
            body()
        } catch (e: SerializationException) {
            throw SerializationException(errorMessage(binary, offset, input), e)
        } catch (e: Throwable) {
            throw AssertionError(errorMessage(binary, offset, input), e)
        }
    }

    private fun errorMessage(binary: Boolean, offset: Int, input: String): String =
        "binary: $binary, offset: $offset, input: '${input.replace("\n", "\\n")}'"

    internal fun test(input: String, offset: Int = Random.nextInt(7), body: JsonReaderImpl.() -> Unit) {
        setStringJson(input, offset)
        enrichError(input, false, offset) { reader.body() }

        setBufferJson(input, offset)
        enrichError(input, true, offset) { reader.body() }
    }

    internal fun testWithInput(input: String, offset: Int = Random.nextInt(7), body: (ZeroTextReader) -> Unit) {
        setStringJson(input, offset)
        enrichError(input, false, offset) { body(stringInput) }

        setBufferJson(input, offset)
        enrichError(input, true, offset) { body(bufferInput) }
    }
}

fun JsonReader.expectString(string: String) {
    assertEquals(string, readString())
}