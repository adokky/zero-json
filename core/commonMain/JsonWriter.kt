package dev.dokky.zerojson

import kotlinx.serialization.json.JsonElement

interface JsonWriter {
    fun beginString()
    fun endString()

    fun writeNumber(num: Float)
    fun writeNumber(num: Double)
    fun writeNumber(num: Long)
    fun writeNumber(num: Int)
    fun writeNumber(num: Short)
    fun writeNumber(num: Byte)

    fun writeNumber(num: ULong)
    fun writeNumber(num: UInt)
    fun writeNumber(num: UShort)
    fun writeNumber(num: UByte)

    fun writeBoolean(bool: Boolean)

    fun writeString(char: Char)
    fun writeString(string: String, start: Int = 0, end: Int = string.length)
    fun writeKey(key: String): Unit = writeString(key)

    fun beginObject(size: Int = 16)
    fun endObject()
    fun beginArray(size: Int = 4)
    fun endArray()
    fun colon()
    fun comma()

    fun writeNull()

    fun write(element: JsonElement, skipNullKeys: Boolean = false)
}

