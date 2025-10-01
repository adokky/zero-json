package dev.dokky.zerojson.internal

import io.kodec.text.TextWriter
import kotlinx.serialization.json.*

internal class JsonTextWriter(
    textWriter: TextWriter,
    val allowNaNs: Boolean = false
): JsonWriterBase(), AutoCloseable {
    var textWriter: TextWriter = textWriter
        set(value) {
            escapingDepth = EscapingDepth.INITIAL
            field = value
        }

    private var escapingDepth = EscapingDepth.INITIAL

    override fun close() {
        escapingDepth = EscapingDepth.INITIAL
    }

    override fun beginString() {
        quotes()
        escapingDepth = EscapingDepth.next(escapingDepth)
    }

    override fun endString() {
        escapingDepth = EscapingDepth.prev(escapingDepth)
        quotes()
    }

    override fun writeNumber(num: Float) {
        if (!allowNaNs && !num.isFinite()) throwNansAreNotAllowed(num)
        textWriter.append(num)
    }

    override fun writeNumber(num: Double) {
        if (!allowNaNs && !num.isFinite()) throwNansAreNotAllowed(num)
        textWriter.append(num)
    }

    override fun writeNumber(num: Long) { textWriter.append(num) }
    override fun writeNumber(num: Int) { textWriter.append(num) }
    override fun writeNumber(num: Short) { textWriter.append(num) }
    override fun writeNumber(num: Byte) { textWriter.append(num) }

    override fun writeNumber(num: ULong) { textWriter.append(num.toString()) }
    override fun writeNumber(num: UInt) { textWriter.append(num.toLong()) }
    override fun writeNumber(num: UShort) { textWriter.append(num.toInt()) }
    override fun writeNumber(num: UByte) { textWriter.append(num.toInt()) }

    override fun writeBoolean(bool: Boolean) { textWriter.append(bool) }

    override fun writeString(char: Char) {
        quotes()
        textWriter.appendEscapedChar(char, escapingDepth)
        quotes()
    }

    override fun writeString(string: String, start: Int, end: Int) {
        quotes()
        textWriter.appendJsonString(string, start, end, escapingDepth)
        quotes()
    }

    override fun beginObject(size: Int) { textWriter.append('{') }
    override fun endObject() { textWriter.append('}') }
    override fun beginArray(size: Int) { textWriter.append('[') }
    override fun endArray() { textWriter.append(']') }
    override fun colon() { textWriter.append(':') }
    override fun comma() { textWriter.append(',') }
    fun quotes() { textWriter.appendQuotes(escapingDepth) }

    override fun writeNull() { textWriter.append("null") }

    override fun write(element: JsonElement, skipNullKeys: Boolean) {
        when(element) {
            is JsonArray -> write(element, skipNullKeys = skipNullKeys)
            is JsonObject -> write(element, null, null, skipNullKeys = skipNullKeys)
            is JsonPrimitive -> write(element)
        }
    }

    fun write(element: JsonPrimitive) {
        val content = element.content

        var depth = escapingDepth
        if (element.isString) quotes() else {
            if (depth == 0) {
                textWriter.append(element.content)
                return
            }
            depth--
        }

        textWriter.appendJsonString(content, start = 0, end = content.length, escapeDepth = depth)
        if (element.isString) quotes()
    }

    fun write(array: JsonArray, skipNullKeys: Boolean) {
        beginArray()
        var comma = false
        for (v in array) {
            if (comma) comma() else comma = true
            write(v, skipNullKeys)
        }
        endArray()
    }

    override fun write(
        element: JsonObject,
        discriminatorKey: String?,
        discriminatorValue: String?,
        skipNullKeys: Boolean
    ) {
        beginObject()

        var comma = false
        if (discriminatorKey != null) {
            writeString(discriminatorKey)
            colon()
            writeString(discriminatorValue!!)
            comma = true
        }

        for ((k, v) in element.entries) {
            if (skipNullKeys && v == JsonNull) continue
            if (comma) comma() else comma = true
            writeString(k)
            colon()
            write(v, skipNullKeys)
        }

        endObject()
    }
}