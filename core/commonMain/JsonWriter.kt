package dev.dokky.zerojson

import kotlinx.serialization.json.JsonElement

/**
 * Interface for writing JSON data in a structured way.
 */
interface JsonWriter {
    /**
     * Begins writing a JSON string value.
     * Typically used to start a string context for writing characters.
     */
    fun beginString()

    /**
     * Ends writing a JSON string value.
     * Completes the string context and performs any necessary escaping.
     */
    fun endString()

    /**
     * Writes a floating-point number as a JSON number value.
     */
    fun writeNumber(num: Float)

    /**
     * Writes a double-precision floating-point number as a JSON number value.
     */
    fun writeNumber(num: Double)

    /**
     * Writes a long integer as a JSON number value.
     */
    fun writeNumber(num: Long)

    /**
     * Writes an integer as a JSON number value.
     */
    fun writeNumber(num: Int)

    /**
     * Writes a short integer as a JSON number value.
     */
    fun writeNumber(num: Short)

    /**
     * Writes a byte as a JSON number value.
     */
    fun writeNumber(num: Byte)

    /**
     * Writes an unsigned long integer as a JSON number value.
     */
    fun writeNumber(num: ULong)

    /**
     * Writes an unsigned integer as a JSON number value.
     */
    fun writeNumber(num: UInt)

    /**
     * Writes an unsigned short integer as a JSON number value.
     */
    fun writeNumber(num: UShort)

    /**
     * Writes an unsigned byte as a JSON number value.
     */
    fun writeNumber(num: UByte)

    /**
     * Writes a boolean value as a JSON boolean.
     */
    fun writeBoolean(bool: Boolean)

    /**
     * Writes a single character as part of a JSON string.
     */
    fun writeString(char: Char)

    /**
     * Writes a string as a JSON string value.
     * @param string the [String] to write
     * @param start the starting index (inclusive) of the substring to write (default: `0`)
     * @param end the ending index (exclusive) of the substring to write (default: `string.length`)
     */
    fun writeString(string: String, start: Int = 0, end: Int = string.length)

    /**
     * Writes a key in a JSON object.
     * By default, delegates to [writeString].
     * @param key the key name to write (without quotes)
     */
    fun writeKey(key: String): Unit = writeString(key)

    /**
     * Begins writing a JSON object.
     * @param size the expected number of key-value pairs (for optimization, default: `16`)
     */
    fun beginObject(size: Int = 16)

    /**
     * Ends writing a JSON object.
     */
    fun endObject()

    /**
     * Begins writing a JSON array.
     * @param size the expected number of elements (for optimization, default: `4`)
     */
    fun beginArray(size: Int = 4)

    /**
     * Ends writing a JSON array.
     */
    fun endArray()

    /**
     * Writes a colon separator between a key and value in a JSON object.
     */
    fun colon()

    /**
     * Writes a comma separator between elements in JSON objects or arrays.
     */
    fun comma()

    /**
     * Writes a JSON `null` value.
     */
    fun writeNull()

    /**
     * Writes a [JsonElement].
     * @param skipNullKeys whether to skip object properties with null values (default: `false`)
     */
    fun write(element: JsonElement, skipNullKeys: Boolean = false)
}

