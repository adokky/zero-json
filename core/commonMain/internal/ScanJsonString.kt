package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import io.kodec.DecodingErrorHandler
import io.kodec.StringHashCode
import io.kodec.StringsUTF16
import io.kodec.text.CharToClassMapper
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.StringTextReader
import io.kodec.text.TextReader
import karamel.utils.*
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

@JvmOverloads
internal fun JsonReaderImpl.scanString(
    requireQuotes: Boolean = config.expectStringQuotes,
    maxLength: Int = ZeroJsonConfiguration.Default.maxStringLength,
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

internal fun JsonReaderImpl.scanKeyword(
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowEscapes: Boolean,
    allowNull: Boolean,
    allowBoolean: Boolean
): ScanResult {
    val start = position
    val result = when (val input = input) {
        is StringTextReader -> input.scanKeywordContent(
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowEscapes = allowEscapes
        )
        else -> input.scanJsonStringContent(
            allowEof = true,
            charClasses = JsonCharClasses.mapper,
            terminatorClass = JsonCharClasses.STR_TERM,
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowEscapes = allowEscapes
        )
    }
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
    val res = when(val input = input) {
        is StringTextReader -> input.scanQuotedStringContent(
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowEscapes = allowEscapes
        )
        else -> input.scanJsonStringContent(
            allowEof = false,
            charClasses = JsonCharClasses.mapper,
            terminatorClass = JsonCharClasses.DOUBLE_QUOTES,
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowEscapes = allowEscapes
        )
    }
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
): ScanResult = scanStringContentTemplate(
    allowEof = allowEof,
    terminator = { cp -> charClasses.hasClass(cp, terminatorClass) },
    maxLength = maxLength,
    onMaxLength = onMaxLength,
    readEscapedChar = { if (allowEscapes) readEscapeChar() else return ScanResult.EscapedString },
    acceptChar = {}
)

internal fun StringTextReader.scanQuotedStringContent(
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowEscapes: Boolean
): ScanResult = scanStringContentTemplate(
    allowEof = false,
    maxLength = maxLength,
    onMaxLength = onMaxLength,
    terminator = { it == '"'.code },
    allowEscapes = allowEscapes
)

private fun StringTextReader.scanKeywordContent(
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    allowEscapes: Boolean
): ScanResult = scanStringContentTemplate(
    allowEof = true,
    maxLength = maxLength,
    onMaxLength = onMaxLength,
    terminator = { JsonCharClasses.mapper.hasClass(it, JsonCharClasses.STR_TERM) },
    allowEscapes = allowEscapes
)

// WARN: Dos not check unfinished surrogate pairs.
// This function is only used for string key matching.
private inline fun StringTextReader.scanStringContentTemplate(
    allowEof: Boolean,
    maxLength: Int,
    onMaxLength: DecodingErrorHandler<String>,
    terminator: (codepoint: Int) -> Boolean,
    allowEscapes: Boolean
): ScanResult {
    val start = position
    var hash = StringHashCode.init()
    var isEscaped = false
    var terminated = false
    var codePoints = 0
    var length = 0

    var i = start
    while (i < input.length) {
        val i0 = i
        var c = input[i]
        when {
            c == '\\' -> {
                if (!allowEscapes) return ScanResult.EscapedString
                isEscaped = true
                position = i + 1
                c = readEscapeChar()
                i = position - 1
            }
            terminator(c.code) -> { terminated = true; break }
        }
        if (++length > maxLength) {
            position = i0 - 1
            onMaxLength(MAX_STRING_LENGTH_ERR_MESSAGE)
            i = i0
            terminated = true
            break
        }
        codePoints += (!c.isLowSurrogate()).toInt()
        hash = StringHashCode.next(hash, c)
        i++
    }
    position = i

    if (!terminated && !allowEof) unexpectedEof()

    return ScanResult(
        codePoints = codePoints,
        computedHashCode = hash,
        isEscaped = isEscaped
    )
}

internal inline fun RandomAccessTextReader.scanStringContentTemplate(
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