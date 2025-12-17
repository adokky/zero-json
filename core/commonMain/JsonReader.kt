package dev.dokky.zerojson

import dev.dokky.zerojson.JsonReader.Companion.startReadingFrom
import dev.dokky.zerojson.internal.JsonReaderImpl
import io.kodec.DecodingErrorHandler
import io.kodec.buffers.Buffer
import kotlin.jvm.JvmStatic

/**
 * Reads JSON tokens from a source.
 *
 * Instances are created via [startReadingFrom] or
 * accessed via [ZeroJsonTextDecoder.reader].
 */
abstract class JsonReader internal constructor(val config: JsonReaderConfig) {
    /**
     * The position of the next codepoint to read.
     *
     * - May start from any value, including EOF (negative).
     * - Becomes negative (typically -1) when EOF is reached.
     *
     * This value can be saved and later used to restore the reader's position
     * via the [position] setter. However, it does not represent a size or offset
     * in any specific unit (e.g. bytes, characters, codepoints).
     * It is an opaque marker intended for position tracking only.
     *
     * ⚠️ Delicate API. Incorrect usage may break the decoding process.
     * If used incorrectly, behavior of the [JsonReader] and any [ZeroJsonDecoder]
     * using it becomes undefined.
     */
    abstract var position: Int

    /**
     * Determines the type of the next JSON value.
     *
     * This function examines the next token in the input stream and returns
     * corresponding [ValueType] without consuming the actual token.
     *
     * @param quickGuess if `true` performs a fast check without exhaustive validation of the next token type.
     * Use with caution, as reading the exact type after this check may result in
     * misleading error messages.
     *
     * For example, the code below will throw `"expected number"`:
     * ```
     * when(JsonReader.startReadingFrom("no_a_JSON").nextValueType(true)) {
     *     ValueType.STRING -> readString()
     *     ValueType.NUMBER -> readInt()
     *     else -> reader.fail("expected string or int")
     * }
     * ```
     */
    abstract fun nextValueType(quickGuess: Boolean = false): ValueType

    enum class ValueType {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    /**
     * Reads a JSON string.
     *
     * @param requireQuotes whether the string must be quoted
     * @param maxLength maximum string length in characters
     * @return the string value without quotes
     */
    abstract fun readString(
        requireQuotes: Boolean = config.expectStringQuotes,
        maxLength: Int = config.maxStringLength
    ): String

    /**
     * Reads a JSON string into [output].
     *
     * @param output the builder to append the string to
     * @param requireQuotes whether the string must be quoted
     * @param maxLength maximum string length in characters
     */
    abstract fun readString(
        output: StringBuilder,
        requireQuotes: Boolean = config.expectStringQuotes,
        maxLength: Int = config.maxStringLength
    )

    /**
     * Reads a JSON number as a float.
     *
     * @param allowSpecial whether to allow `NaN`, `Infinity`, `-Infinity`
     * @param skipWhitespace whether to skip whitespace after the number
     * @return the float value
     */
    abstract fun readFloat(
        allowSpecial: Boolean = config.allowSpecialFloatingPointValues
    ): Float

    /**
     * Reads a JSON number as a double.
     *
     * @param allowSpecial whether to allow `NaN`, `Infinity`, `-Infinity`
     * @param skipWhitespace whether to skip whitespace after the number
     * @return the double value
     */
    abstract fun readDouble(
        allowSpecial: Boolean = config.allowSpecialFloatingPointValues
    ): Double

    /**
     * Reads a JSON boolean.
     *
     * @return the boolean value
     */
    abstract fun readBoolean(): Boolean

    /**
     * Reads a JSON number as a byte.
     *
     * @param skipWhitespace whether to skip whitespace after the number
     */
    abstract fun readByte(): Byte

    /**
     * Reads a JSON number as a short.
     *
     * @param skipWhitespace whether to skip whitespace after the number
     */
    abstract fun readShort(): Short

    /**
     * Reads a JSON number as an int.
     *
     * @param skipWhitespace whether to skip whitespace after the number
     */
    abstract fun readInt(): Int

    /**
     * Reads a JSON number as a long.
     *
     * @param skipWhitespace whether to skip whitespace after the number
     */
    abstract fun readLong(): Long

    /**
     * Reads a JSON null.
     *
     * @throws ZeroJsonDecodingException if the next token is not null
     */
    abstract fun readNull()

    /**
     * Tries to skip a JSON null.
     *
     * @return `true` if null was skipped, `false` otherwise
     */
    abstract fun trySkipNull(): Boolean

    /**
     * Skips any JSON value: string, number, object, array, boolean or null.
     *
     * Does NOT validate the skipped value.
     */
    abstract fun skipElement()

    /**
     * Skips any JSON value: string, number, object, array, boolean or null.
     *
     * Does NOT validate the skipped value.
     */
    abstract fun skipElementUnsafe()

    /**
     * Expects the next token to be [char].
     *
     * @param char the expected character
     * @throws ZeroJsonDecodingException if the next token is not [char]
     */
    abstract fun expectToken(char: Char)

    /**
     * Expects end of input.
     *
     * @throws ZeroJsonDecodingException if there are more tokens
     */
    abstract fun expectEof()

    /**
     * Tries to skip a single-character JSON token [char].
     *
     * @param char the character to skip
     * @return `true` if [char] was skipped, `false` otherwise
     */
    abstract fun trySkipToken(char: Char): Boolean

    /**
     * Checks if the next token is [char].
     *
     * @param char the character to check
     * @return `true` if next token is [char], `false` otherwise
     */
    abstract fun nextIs(char: Char): Boolean

    /**
     * The next code point in the input.
     */
    abstract val nextCodePoint: Int

    /**
     * Expects the next code point to be [char].
     *
     * @param char the expected character
     * @throws ZeroJsonDecodingException if the next code point is not [char]
     */
    abstract fun expectNextIs(char: Char)

    /**
     * Skips all whitespace characters.
     */
    abstract fun skipWhitespace()

    /** Expects '{' */
    fun expectBeginObject(): Unit = expectToken('{')

    /** Expects '[' */
    fun expectBeginArray(): Unit = expectToken('[')

    /** Expects '}' */
    fun expectEndObject(): Unit = expectToken('}')

    /** Expects ']' */
    fun expectEndArray(): Unit = expectToken(']')

    /** Expects ':' */
    fun expectColon(): Unit = expectToken(':')

    /** Expects ',' */
    fun expectComma(): Unit = expectToken(',')

    /** Tries to skip ',' */
    fun trySkipComma(): Boolean = trySkipToken(',')

    /**
     * Default error handler that throws [ZeroJsonDecodingException].
     */
    abstract val fail: DecodingErrorHandler<Any>

    /**
     * Throws [ZeroJsonDecodingException] with [message].
     */
    abstract fun fail(message: String): Nothing

    companion object {
        /**
         * Creates a [JsonReader] that reads from [input].
         *
         * @param input the buffer to read from
         * @param config the reader configuration
         */
        @JvmStatic
        fun startReadingFrom(input: Buffer, config: JsonReaderConfig = JsonReaderConfig()): JsonReader =
            JsonReaderImpl.startReadingFrom(input, config)

        /**
         * Creates a [JsonReader] that reads from [input].
         *
         * @param input the character sequence to read from
         * @param config the reader configuration
         */
        @JvmStatic
        fun startReadingFrom(input: CharSequence, config: JsonReaderConfig = JsonReaderConfig()): JsonReader =
            JsonReaderImpl.startReadingFrom(input, config)
    }
}

/**
 * Reads a nullable value.
 *
 * @param read function to read the non-null value
 * @return the value or `null`
 */
inline fun <T: Any> JsonReader.readNullable(read: JsonReader.() -> T): T? =
    if (trySkipNull()) null else read()