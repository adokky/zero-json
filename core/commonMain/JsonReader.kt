package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import io.kodec.DecodingErrorHandler
import io.kodec.buffers.Buffer
import kotlin.jvm.JvmStatic

abstract class JsonReader internal constructor(val config: JsonReaderConfig) {
    abstract fun readString(requireQuotes: Boolean = config.expectStringQuotes): String

    /** @return hash code */
    abstract fun readString(output: StringBuilder, requireQuotes: Boolean = config.expectStringQuotes): Int

    abstract fun readFloat(allowSpecial: Boolean = config.allowSpecialFloatingPointValues, skipWhitespace: Boolean = true): Float

    abstract fun readDouble(allowSpecial: Boolean = config.allowSpecialFloatingPointValues, skipWhitespace: Boolean = true): Double

    /**
     * @param skipWhitespace if `true` skips all white space characters `after` the boolean
     */
    abstract fun readBoolean(skipWhitespace: Boolean = true): Boolean

    /**
     * @param skipWhitespace if `true` skips all white space characters `after` the boolean
     * @return `null` if next JSON token is not a boolean
     */
    abstract fun tryReadBoolean(skipWhitespace: Boolean = true): Boolean?

    /**
     * @param skipWhitespace if `true` skips all white space characters `after` the number
     */
    abstract fun readByte(skipWhitespace: Boolean = true): Byte

    /**
     * @param skipWhitespace if `true` skips all white space characters `after` the number
     */
    abstract fun readShort(skipWhitespace: Boolean = true): Short

    /**
     * @param skipWhitespace if `true` skips all white space characters `after` the number
     */
    abstract fun readInt(skipWhitespace: Boolean = true): Int

    /**
     * @param skipWhitespace if `true` skips all white space characters `after` the number
     */
    abstract fun readLong(skipWhitespace: Boolean = true): Long

    abstract fun readNull()

    abstract fun trySkipNull(): Boolean

    /**
     * Skips any JSON value: string, number, object, array, boolean and `null`.
     *
     * Does NOT check the validity of number, boolean and null.
     */
    abstract fun skipElement()

    abstract fun expectToken(char: Char)

    abstract fun expectEof()

    /** Tries to skip single-char JSON token [char] (comma, colon, bracket, etc.) */
    abstract fun trySkipToken(char: Char): Boolean

    abstract fun nextIs(char: Char): Boolean

    abstract val nextCodePoint: Int

    abstract fun expectNextIs(char: Char)

    abstract fun skipWhitespace()

    fun expectBeginObject(): Unit = expectToken('{')
    fun expectBeginArray(): Unit = expectToken('[')

    fun expectEndObject(): Unit = expectToken('}')
    fun expectEndArray(): Unit = expectToken(']')

    fun expectColon(): Unit = expectToken(':')
    fun expectComma(): Unit = expectToken(',')

    fun trySkipComma(): Boolean = trySkipToken(',')

    /**
     * Default error handler which throwing [ZeroJsonDecodingException].
     *
     * Error message will be:
     * * [Throwable.message] if `cause` is [Throwable]
     * * [io.kodec.DecodingErrorWithMessage.message] if `cause` is [io.kodec.DecodingErrorWithMessage]
     * * [kotlin.reflect.KClass.qualifiedName] name of `cause::class` if it is not `null`
     * * [kotlin.reflect.KClass.simpleName] name of `cause::class` in all other cases
     */
    abstract val fail: DecodingErrorHandler<Any>

    /**
     * Throws [ZeroJsonDecodingException] with specified [message]
     */
    abstract fun fail(message: String): Nothing

    companion object {
        @JvmStatic
        fun startReadingFrom(input: Buffer, config: JsonReaderConfig = JsonReaderConfig.Default): JsonReader =
            JsonReaderImpl.startReadingFrom(input, config)

        @JvmStatic
        fun startReadingFrom(input: CharSequence, config: JsonReaderConfig = JsonReaderConfig.Default): JsonReader =
            JsonReaderImpl.startReadingFrom(input, config)
    }
}

inline fun <T: Any> JsonReader.readNullable(read: JsonReader.() -> T): T? = if (trySkipNull()) null else read()