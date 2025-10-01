package dev.dokky.zerojson.internal

import io.kodec.DecodingErrorHandler
import io.kodec.StringHashCode
import io.kodec.StringsUTF16
import io.kodec.text.CharToClassMapper
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.TextReader
import karamel.utils.*
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Used only in [readString]. Marked as internal for testing purposes.
 *
 * WARN: the function does NOT:
 * * check validity of the escape sequences
 * * skip any white spaces
 */
@JvmOverloads
internal fun JsonReaderImpl.scanString(
    requireQuotes: Boolean = config.expectStringQuotes,
    maxLength: Int = DEFAULT_MAX_STRING_LENGTH,
    onMaxLength: DecodingErrorHandler<String>,
    allowEscapes: Boolean = true,
    allowNull: Boolean = false,
    allowBoolean: Boolean = true
): ScanResult {
    val quotes = when {
        requireQuotes -> { expectOpenQuotes(); true }
        else -> input.trySkip('"')
    }
    return when {
        quotes -> scanQuotedString(
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowEscapes = allowEscapes
        )
        else -> scanKeyword(
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowEscapes = allowEscapes,
            allowNull = allowNull,
            allowBoolean = allowBoolean
        )
    }
}

private fun JsonReaderImpl.scanKeyword(
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowEscapes: Boolean,
    allowNull: Boolean,
    allowBoolean: Boolean
): ScanResult {
    val start = position
    val result = input.scanJsonStringContent(
        allowEof = true,
        charClasses = JsonCharClasses.mapper,
        terminatorClass = JsonCharClasses.STR_TERM,
        maxLength = maxLength,
        onMaxLength = onMaxLength,
        allowEscapes = allowEscapes
    )
    checkUnquotedString(input,
        start = start,
        hash = result.hash,
        allowNull = allowNull,
        allowBoolean = allowBoolean
    )
    return result
}

private fun JsonReaderImpl.scanQuotedString(
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowEscapes: Boolean
): ScanResult {
    val res = input.scanJsonStringContent(
        allowEof = false,
        charClasses = JsonCharClasses.mapper,
        terminatorClass = JsonCharClasses.DOUBLE_QUOTES,
        maxLength = maxLength,
        onMaxLength = onMaxLength,
        allowEscapes = allowEscapes
    )
    input.trySkip('"')
    return res.markQuoted()
}

internal fun <BDS : BitDescriptors> RandomAccessTextReader.scanJsonStringContent(
    allowEof: Boolean,
    allowEscapes: Boolean,
    charClasses: CharToClassMapper<BDS>,
    terminatorClass: Bits32<BDS>,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>
): ScanResult = scanJsonStringContentTemplate(
    allowEof,
    terminator = { cp -> charClasses.hasClass(cp, terminatorClass) },
    maxLength = maxLength,
    onMaxLength = onMaxLength,
    readEscapedChar = { if (allowEscapes) readEscapeChar() else return ScanResult.EscapedString },
    acceptChar = {}
)

internal inline fun RandomAccessTextReader.scanJsonStringContentTemplate(
    allowEof: Boolean,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    terminator: (codepoint: Int) -> Boolean,
    readEscapedChar: TextReader.() -> Char,
    acceptChar: (Char) -> Unit
): ScanResult {
    var prevPos = position
    var codePoints = 0
    var charCount = 0
    var hash = StringHashCode.init()
    var isEscaped = false

    while (true) {
        if (terminator(nextCodePoint)) break

        var cp = readCodePoint()
        when {
            cp < 0 -> if (allowEof) break else unexpectedEof()
            cp == '\\'.code -> {
                cp = readEscapedChar().code
                isEscaped = true
            }
            cp >= StringsUTF16.MIN_SUPPLEMENTARY_CODE_POINT -> {
                val hs = StringsUTF16.highSurrogateCharCode(cp)
                hash = StringHashCode.next(hash, hs)
                acceptChar(hs.toChar())
                charCount++
                cp = StringsUTF16.lowSurrogateCharCode(cp)
            }
        }
        if (++charCount > maxLength) { position = prevPos; onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE); break }
        codePoints++

        hash = StringHashCode.next(hash, cp)
        acceptChar(cp.toChar())
        prevPos = position
    }

    return ScanResult(
        codePoints = codePoints,
        computedHashCode = hash,
        isEscaped = isEscaped
    )
}

/**
 * @property isEscaped `true` if string contains at least one escape sequence
 * @property quoted `1` if string is decorated with double quotes (`"`), otherwise `0`
 * @property hash regular [String] hash code
 * @property codePoints number of UTF code points, not including quotes (`"`)
 */
@JvmInline
internal value class ScanResult private constructor(private val asLong: Long) {
    constructor(codePoints: Int, computedHashCode: Int): this(
        codePoints = codePoints,
        computedHashCode = computedHashCode,
        isEscaped = false
    )

    constructor(codePoints: Int, computedHashCode: Int, isEscaped: Boolean): this(
        (codePoints.toLong() shl 34) or
        (isEscaped.toLong()  shl 33) or
        computedHashCode.asLong()
    )

    val codePoints: Int get() = (asLong ushr 34).toInt()
    val hash: Int get() = asLong.toInt()
    val quoted: Int get() = (asLong shr 32).toInt() and 1
    val isEscaped: Boolean get() = ((asLong shr 33) and 1).toBoolean()

    fun markQuoted(): ScanResult = ScanResult(asLong or (1L shl 32))

    override fun toString(): String = when {
        isEscaped -> "(escaped string)"
        else -> "(codePoints=$codePoints, hashCode=$hash, quoted=$quoted)"
    }

    companion object {
        @JvmStatic
        val EscapedString: ScanResult = ScanResult(1L shl 33)
    }
}