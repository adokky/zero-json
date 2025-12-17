@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.dokky.zerojson.internal

import dev.dokky.zerojson.JsonMaxDepthReachedException
import dev.dokky.zerojson.JsonNumberIsOutOfRange
import dev.dokky.zerojson.JsonReader
import dev.dokky.zerojson.JsonReaderConfig
import io.kodec.DecodingErrorHandler
import io.kodec.buffers.Buffer
import io.kodec.text.*
import karamel.utils.assert
import karamel.utils.unsafeCast
import kotlinx.serialization.InternalSerializationApi
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

// WARN! ChunkedDecoder spec defines hard limit = 16384
private const val STRING_CHUNK_SIZE = 1024

internal class JsonReaderImpl private constructor(
    internal var input: RandomAccessTextReader,
    config: JsonReaderConfig
): JsonReader(config) {
    internal constructor(
        input: ZeroTextReader,
        config: JsonReaderConfig
    ): this(input.unsafeCast<RandomAccessTextReader>(), config)

    override var position: Int
        get() = input.position
        set(value) { input.position = value }

    internal val readNumberResult = ReadNumberResult()

    override fun readString(requireQuotes: Boolean, maxLength: Int): String = config.stringBuilder.buildString {
        readString(this, requireQuotes = requireQuotes, maxLength = maxLength)
    }

    /** @return hash code */
    override fun readString(output: StringBuilder, requireQuotes: Boolean, maxLength: Int) {
        input.readJsonString(output, requireQuotes = requireQuotes, maxLength = maxLength)
            .also { skipWhitespace() }
    }

    private class ChunkedReadMaxLengthHandler: DecodingErrorHandler<Any> {
        var hasMoreChunks = false
        override fun invoke(cause: Any) { hasMoreChunks = true }
    }

    private val chunkedReadMaxLengthHandler = ChunkedReadMaxLengthHandler()

    fun readStringChunked(
        buffer: StringBuilderWrapper,
        requireQuotes: Boolean = config.expectStringQuotes,
        chuckSize: Int = STRING_CHUNK_SIZE,
        acceptChunk: (String) -> Unit
    ) {
        val bufStart = buffer.builder.length
        var expectQuotes = if (requireQuotes || input.nextCodePoint == '"'.code) 1 else 0
        do {
            chunkedReadMaxLengthHandler.hasMoreChunks = false
            input.readJsonString(
                output = buffer.builder,
                requireQuotes = expectQuotes == 1,
                maxLength = chuckSize,
                onMaxLength = chunkedReadMaxLengthHandler
            )
            acceptChunk(buffer.removeSubstring(startIndex = bufStart))
            expectQuotes += (expectQuotes and 1) shl 1
        } while (chunkedReadMaxLengthHandler.hasMoreChunks)
        if (expectQuotes > 0b11) expectCloseQuotes()
        skipWhitespace()
    }

    private var positionBeforeNumber = 0

    private val integerFormatErrorHandler = DecodingErrorHandler<IntegerParsingError> { cause ->
        input.position = positionBeforeNumber
        when (cause) {
            IntegerParsingError.MalformedNumber -> throwExpectedNumber("integer")
            IntegerParsingError.Overflow -> throw JsonNumberIsOutOfRange(Int.MIN_VALUE, Int.MAX_VALUE)
        }
    }

    private val floatFormatErrorHandler = DecodingErrorHandler<Any> { _ ->
        input.position = positionBeforeNumber
        throwExpectedNumber("number")
    }

    private fun throwExpectedNumber(numberType: String): Nothing {
        input.fail(config.messageBuilder.buildString {
            append("expected ")
            append(numberType)
            append(", got ")
            appendNextJsonTokenFrom(input)
        })
    }

    override fun readBoolean(): Boolean = readBoolean(skipWhitespace = true)
    override fun readByte(): Byte = readByte(skipWhitespace = true)
    override fun readShort(): Short = readShort(skipWhitespace = true)
    override fun readInt(): Int = readInt(skipWhitespace = true)
    override fun readLong(): Long = readLong(skipWhitespace = true)
    override fun readFloat(allowSpecial: Boolean): Float = readFloat(allowSpecial, skipWhitespace = true)
    override fun readDouble(allowSpecial: Boolean): Double = readDouble(allowSpecial, skipWhitespace = true)

    fun readFloat(allowSpecial: Boolean, skipWhitespace: Boolean): Float {
        positionBeforeNumber = position
        val result = input.readFloat(onFormatError = floatFormatErrorHandler, allowSpecialValues = true).also { v ->
            if (!allowSpecial && !v.isFinite()) specialFloatsProhibited(v.toDouble())
        }
        if (skipWhitespace) skipWhitespace()
        return result
    }

    fun readDouble(allowSpecial: Boolean, skipWhitespace: Boolean): Double {
        positionBeforeNumber = position
        val result = input.readDouble(onFormatError = floatFormatErrorHandler, allowSpecialValues = true).also { v ->
            if (!allowSpecial && !v.isFinite()) specialFloatsProhibited(v)
        }
        if (skipWhitespace) skipWhitespace()
        return result
    }

    private fun specialFloatsProhibited(v: Double) {
        position = positionBeforeNumber
        throwNansAreNotAllowed(v, position = positionBeforeNumber)
    }

    fun readByte(skipWhitespace: Boolean): Byte {
        val v = readLong(skipWhitespace)
        if (v.toByte().toLong() != v) throw JsonNumberIsOutOfRange(
            Byte.MIN_VALUE,
            Byte.MAX_VALUE,
            position = positionBeforeNumber
        )
        return v.toByte()
    }

    fun readShort(skipWhitespace: Boolean): Short {
        val v = readLong(skipWhitespace)
        if (v.toShort().toLong() != v) throw JsonNumberIsOutOfRange(
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            position = positionBeforeNumber
        )
        return v.toShort()
    }

    fun readInt(skipWhitespace: Boolean): Int {
        val v = readLong(skipWhitespace)
        if (v.toInt().toLong() != v) throw JsonNumberIsOutOfRange(
            Int.MIN_VALUE,
            Int.MAX_VALUE,
            position = positionBeforeNumber
        )
        return v.toInt()
    }

    fun readLong(skipWhitespace: Boolean): Long {
        positionBeforeNumber = input.position
        return input.readLong(onFormatError = integerFormatErrorHandler).also {
            if (skipWhitespace) skipWhitespace()
        }
    }

    override fun readNull() {
        if (!trySkipNull()) input.fail("'null' is expected")
    }

    override fun trySkipNull(): Boolean = input.trySkipJsonNull().also { if (it) skipWhitespace() }

    /**
     * If [afterColon] is `false` then requires [nextCodePoint] to be colon ('`:`').
     * Trailing comma is supported.
     *
     * @return `false` if there are no more elements
     * (the function stops right before the closing bracket)
     */
    fun skipToNextKey(afterColon: Boolean = true): Boolean {
        if (!afterColon) expectColon()
        skipElement()
        return trySkipObjectComma()
    }

    override fun skipElement() {
        skipElement(unchecked = false)
    }

    override fun skipElementUnsafe() {
        skipElement(unchecked = true)
    }

    fun skipElement(unchecked: Boolean) {
        val next = nextCodePoint
        when {
            next == '\"'.code -> if (unchecked) skipStringUnsafe() else skipString()
            next.isOpeningBracket() -> if (unchecked) skipObjectOrArrayFast() else skipObjectOrArray()
            else -> {
                if (config.expectStringQuotes) skipUnquotedLiteral() else skipUnquotedLiteralUnsafe()
                skipWhitespace()
            }
        }
    }

    private fun skipUnquotedLiteral() {
        if (input.trySkipBooleanOrNullFast()) return
        if (trySkipNumber(allowSpecialFp = config.allowSpecialFloatingPointValues)) return
        unexpectedChar(nextCodePoint)
    }

    /**
     * @param openingBracket if the parameter is set, the function
     * will expect the [position] has set to the beginning of the first element,
     * not the opening bracket.
     */
    fun skipObjectOrArray(openingBracket: Char = Char(0)) {
        val bracket = when (openingBracket) {
            Char(0) -> readCodePoint().also { skipWhitespace() }
            else -> openingBracket.code
        }

        assert { bracket.isOpeningBracket() }

        when (bracket) {
            '{'.code -> while (true) {
                if (nextCodePoint == '}'.code) { readCodePoint(); break }
                skipString()
                expectColon()
                skipElement()
                trySkipComma()
            }
            else -> while (true) {
                if (nextCodePoint == ']'.code) { readCodePoint(); break }
                skipElement()
                trySkipComma()
            }
        }

        skipWhitespace()
    }

    fun unexpectedChar(next: Int): Nothing {
        input.unexpectedChar(config.messageBuilder, next)
    }

    private val bracketStack = UByteArray(config.depthLimit)

    /**
     * Fast and less safe version of [skipObjectOrArray].
     * The function skips a lot of JSON validity checks.
     * Does not use recursion - SO-safe.
     */
    fun skipObjectOrArrayFast(openingBracket: Char = Char(0)) {
        val firstBracket = when (openingBracket) {
            Char(0) -> readCodePoint().toChar()
            else -> openingBracket
        }

        assert { firstBracket.code.isOpeningBracket() }

        var stackSize = 0
        bracketStack[stackSize++] = firstBracket.code.toClosingBracket().toUByte()

        while (stackSize > 0) {
            val c = nextCodePoint
            when {
                c == -1 -> input.unexpectedEof()
                c == '\"'.code -> { skipStringUnsafe(); continue }
                c.isOpeningBracket() -> {
                    if (stackSize == bracketStack.size) throw JsonMaxDepthReachedException()
                    bracketStack[stackSize++] = c.toClosingBracket().toUByte()
                }
                c.isClosingBracket() -> {
                    val expected = bracketStack[--stackSize].toInt()
                    if (expected != c) input.unexpectedNextChar(config.messageBuilder, expected = expected)
                }
            }
            readCodePoint()
        }

        skipWhitespace()
    }

    /**
     * Skips to next array element or next object key.
     * The function is quite fast but skips a lot of JSON validity checks.
     *
     * @return `false` if there are no more elements (function stops at closing bracket or EOF)
     */
    fun skipToNextItem(allowEof: Boolean = false): Boolean {
        skipWhitespace()

        do {
            val c = nextCodePoint
            when {
                c == '\"'.code -> skipString()
                c.isOpeningBracket() -> skipObjectOrArrayFast()
                c.isClosingBracket() -> return false
                else -> if (readCodePoint() == ','.code) { skipWhitespace(); break }
            }
        } while (c >= 0)

        return when {
            nextCodePoint < 0 -> if (allowEof) false else input.unexpectedEof()
            else -> !nextCodePoint.isClosingBracket()
        }
    }

    override fun expectToken(char: Char) {
        input.expect(char)
        skipWhitespace()
    }

    override fun expectEof() { input.expectEof() }

    fun expectOpenQuotes() = input.expect('"')

    fun expectCloseQuotes() {
        val pos = position
        val next = readCodePoint()
        if (next != '"'.code) {
            position = pos
            input.unexpectedChar(config.messageBuilder, next)
        }
    }

    override fun trySkipToken(char: Char): Boolean = input.trySkip(char).also { if (it) skipWhitespace() }

    override fun nextIs(char: Char): Boolean = input.nextIs(char)

    override val nextCodePoint: Int get() = input.nextCodePoint

    internal fun readCodePoint(): Int = input.readCodePoint()

    override fun expectNextIs(char: Char) { input.expectNextIs(char) }

    override fun skipWhitespace() { input.skipJsonWhitespace(allowComments = config.allowComments) }

    fun readBoolean(skipWhitespace: Boolean): Boolean = tryReadBoolean(skipWhitespace) ?: throwExpectedBoolean()

    fun tryReadBoolean(skipWhitespace: Boolean = true): Boolean? = input.tryReadJsonBoolean()
        ?.also { if (skipWhitespace) skipWhitespace() }

    private fun throwExpectedBoolean(): Nothing = input.fail("expected 'true' or 'false'")

    override val fail: DecodingErrorHandler<Any> get() = input.fail
    override fun fail(message: String): Nothing = input.fail(message)

    override fun nextValueType(quickGuess: Boolean): ValueType = when {
        quickGuess && config.expectStringQuotes -> nextValueTypeFast()
        else -> nextUnquotedValueTypeSlow(quickGuess)
    }

    private fun nextValueTypeFast(): ValueType = when(nextCodePoint) {
        '"'.code -> ValueType.STRING
        '{'.code -> ValueType.OBJECT
        '['.code -> ValueType.ARRAY
        'n'.code -> ValueType.NULL
        't'.code, 'f'.code  -> ValueType.BOOLEAN
        else -> ValueType.NUMBER
    }

    private fun nextUnquotedValueTypeSlow(quickGuess: Boolean): ValueType {
        val start = position

        when(nextCodePoint) {
            '"'.code -> return ValueType.STRING
            '{'.code -> return ValueType.OBJECT
            '['.code -> return ValueType.ARRAY
            'n'.code -> when {
                quickGuess && config.expectStringQuotes -> return ValueType.NULL
                else -> if (trySkipNull()) {
                    position = start
                    return ValueType.NULL
                }
            }
            't'.code, 'f'.code -> when {
                quickGuess && config.expectStringQuotes -> return ValueType.BOOLEAN
                else -> if (tryReadBoolean(skipWhitespace = false) != null) {
                    position = start
                    return ValueType.BOOLEAN
                }
            }
        }

        if (!quickGuess && !trySkipNumber(config.allowSpecialFloatingPointValues)) {
            unexpectedChar(nextCodePoint)
        }
        position = start
        return ValueType.NUMBER
    }

    companion object {
        private fun startReadingFrom(input: ZeroTextReader, config: JsonReaderConfig) =
            JsonReaderImpl(input, config).also { it.skipWhitespace() }

        @JvmStatic
        internal fun startReadingFrom(input: Buffer, config: JsonReaderConfig): JsonReaderImpl {
            val textReader = ZeroUtf8TextReader(errorMessageBuilder = config.messageBuilder)
            textReader.startReadingFrom(input)
            return startReadingFrom(textReader, config)
        }

        @JvmStatic
        internal fun startReadingFrom(input: CharSequence, config: JsonReaderConfig): JsonReaderImpl {
            val textReader = ZeroStringTextReader(errorMessageBuilder = config.messageBuilder)
            textReader.startReadingFrom(input)
            return startReadingFrom(textReader, config)
        }
    }
}

internal fun TextReader.nextIsStringTerminator(): Boolean = JsonCharClasses.isStringTerminator(nextCodePoint)

internal fun TextReader.unexpectedEof(): Nothing = fail("unexpected EOF")

@JvmOverloads
internal inline fun <R> JsonReaderImpl.maybeQuoted(allowQuotes: Boolean = true, body: JsonReaderImpl.() -> R): R {
    val quoted = allowQuotes && input.trySkip('"')
    val result = body()
    if (quoted) expectCloseQuotes()
    skipWhitespace()
    return result
}

@InternalSerializationApi
fun JsonReader.readLong(skipWhitespace: Boolean) = (this as JsonReaderImpl).readLong(skipWhitespace)