package dev.dokky.zerojson.internal

import dev.dokky.zerojson.JsonMaxDepthReachedException
import io.kodec.text.StringTextWriter
import kotlinx.serialization.json.*

/**
 * @param textWriter used for encoding compound JSON inside key
 */
internal class JsonTreeWriter(
    private val stringBuilder: StringBuilder,
    maxDepth: Int,
    private val allowNaNs: Boolean
): JsonWriterBase(), AutoCloseable {
    private val textWriter = JsonTextWriter(StringTextWriter(stringBuilder))
    private var nesting = 0

    private class Frame {
        var array: ArrayList<JsonElement>? = null
        var obj: LinkedHashMap<String, JsonElement>? = null
        var key: String? = null

        fun putKey(key: String) {
            obj ?: error("current element is not JsonObject")
            this.key = key
        }

        fun putElement(element: JsonElement) {
            val obj = obj
            if (obj == null) {
                (array ?: error("frame is not initialized")).add(element)
            } else {
                val key = this.key
                this.key = if (key == null) {
                    element.jsonPrimitive.content
                } else {
                    obj[key] = element
                    null
                }
            }
        }

        val size: Int get() = obj?.size ?: array?.size ?: -1

        fun clear() {
            array = null
            obj = null
            key = null
        }
    }

    private val stack = Array(maxDepth) { Frame() }
    private var stackSize = 1
    init { stack[0].array = ArrayList(1) }

    private fun lastFrame(): Frame = stack[stackSize - 1]

    private fun putElement(element: JsonElement) {
        lastFrame().putElement(element)
    }

    fun beginEncoding() {
        stringBuilder.setLength(0)
        stackSize = 1
        lastFrame().array!!.clear()
    }

    fun endEncoding(): JsonElement {
        check(stackSize == 1) { "unfinished JSON structure" }
        return lastFrame().array!!.first()
    }

    override fun close() {
        nesting = 0
        for (i in 1 until stackSize) {
            stack[i].clear()
        }
        stackSize = 1
        stack[0].array!!.clear()
        textWriter.close()
    }

    override fun beginString() {
        if (++nesting > 1) textWriter.beginString()
    }

    override fun endString() {
        nesting--
        when {
            nesting >= 1 -> textWriter.endString()
            nesting == 0 -> {
                lastFrame().key = stringBuilder.toString()
                stringBuilder.setLength(0)
            }
        }
    }

    override fun writeNumber(num: Float) {
        if (nesting > 0) return textWriter.writeNumber(num)
        if (!allowNaNs && !num.isFinite()) throwNansAreNotAllowed(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: Double) {
        if (nesting > 0) return textWriter.writeNumber(num)
        if (!allowNaNs && !num.isFinite()) throwNansAreNotAllowed(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: Long) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: Int) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: Short) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: Byte) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: ULong) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: UInt) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: UShort) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeNumber(num: UByte) {
        if (nesting > 0) return textWriter.writeNumber(num)
        putElement(JsonPrimitive(num))
    }

    override fun writeBoolean(bool: Boolean) {
        if (nesting > 0) return textWriter.writeBoolean(bool)
        putElement(JsonPrimitive(bool))
    }

    override fun writeString(char: Char) {
        if (nesting > 0) return textWriter.writeString(char)
        putElement(JsonPrimitive(char.toString()))
    }

    override fun writeString(string: String, start: Int, end: Int) {
        if (nesting > 0) return textWriter.writeString(string, start, end)
        putElement(JsonPrimitive(string.substring(start, end)))
    }

    override fun writeKey(key: String) {
        if (nesting > 0) return textWriter.writeKey(key)
        lastFrame().putKey(key)
    }

    override fun beginObject(size: Int) {
        if (nesting > 0) return textWriter.beginObject(size)
        addFrame().obj = LinkedHashMap(size)
    }

    override fun beginArray(size: Int) {
        if (nesting > 0) return textWriter.beginArray(size)
        addFrame().array = ArrayList(size)
    }

    private fun addFrame(): Frame {
        if (stackSize >= stack.size) throw JsonMaxDepthReachedException()
        return stack[stackSize++]
    }

    private inline fun <T: Any> popFrame(
        errorMessage: String,
        data: (Frame) -> T?,
        result: (T) -> JsonElement
    ) {
        check(stackSize > 1) { errorMessage }
        val removed = stack[stackSize - 1]
        val res = result(data(removed) ?: error(errorMessage))
        removed.clear()
        stackSize-- // pop frame only after cleaning
        putElement(res)
    }

    override fun endObject() {
        if (nesting > 0) return textWriter.endObject()
        popFrame(
            errorMessage = "unpaired endObject()",
            data = { it.obj },
            result = { JsonObject(it) }
        )
    }

    override fun endArray() {
        if (nesting > 0) return textWriter.endArray()
        popFrame(
            errorMessage = "unpaired endArray()",
            data = { it.array },
            result = { JsonArray(it) }
        )
    }

    override fun colon() {
        if (nesting > 0) return textWriter.colon()
        debugAssert { lastFrame().key != null }
    }

    override fun comma() {
        if (nesting > 0) return textWriter.comma()
        debugAssert { lastFrame().size > 0 }
    }

    override fun writeNull() {
        if (nesting > 0) return textWriter.writeNull()
        putElement(JsonNull)
    }

    override fun write(element: JsonElement, skipNullKeys: Boolean) {
        if (nesting > 0) return textWriter.write(element, skipNullKeys)

        putElement(if (!skipNullKeys) element else when(element) {
            is JsonObject -> transform(element, null, null, skipNullKeys)
            is JsonArray -> element.filterNullKeys()
            else -> element
        })
    }

    private fun JsonArray.filterNullKeys(): JsonArray {
        var i = indexOfFirst { it is JsonObject }
        if (i < 0) return this

        val size = size
        val arr = ArrayList<JsonElement>(size)

        for (j in 0 until i) {
            arr.add(this[j])
        }

        while (i < size) {
            val element = this[i]
            arr[i] = when (element) {
                !is JsonObject -> element
                else -> transform(element, null, null, skipNullKeys = true)
            }
            i++
        }

        return JsonArray(arr)
    }

    override fun write(
        element: JsonObject,
        discriminatorKey: String?,
        discriminatorValue: String?,
        skipNullKeys: Boolean
    ) {
        if (nesting > 0) return textWriter.write(element, discriminatorKey, discriminatorValue, skipNullKeys)

        putElement(when {
            discriminatorKey == null && !skipNullKeys -> element
            else -> transform(element, discriminatorKey, discriminatorValue, skipNullKeys)
        })
    }

    private fun transform(
        element: JsonObject,
        discriminatorKey: String?,
        discriminatorValue: String?,
        skipNullKeys: Boolean
    ): JsonObject = JsonObject(
        LinkedHashMap<String, JsonElement>(element.size + 1).also { newMap ->
            if (discriminatorKey != null) {
                newMap[discriminatorKey] = JsonPrimitive(discriminatorValue)
            }

            if (skipNullKeys && element.any { it.value == JsonNull }) {
                for ((k, v) in element) {
                    if (v != JsonNull) newMap[k] = v
                }
                return@also
            }

            if (discriminatorKey == null) return element
            newMap.putAll(element)
        }
    )
}