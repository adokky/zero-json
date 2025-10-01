package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonElement
import io.kodec.DecodingErrorHandler
import io.kodec.ErrorContainer
import io.kodec.text.*

internal fun RandomAccessTextReader.tryReadJsonNumber(
    resultBuffer: ReadNumberResult,
    allowSpecialFloatingPointValues: Boolean
): ZeroJsonElement? {
    val firstChar = nextCodePoint

    if (!DefaultCharClasses.isDigit(firstChar) &&
        firstChar != '-'.code &&
        firstChar != '.'.code)
        return null

    // all subsequent decoding logic is extracted to help JIT with inlining
    return tryActuallyReadNumber(resultBuffer, allowSpecialFloatingPointValues)
}

private fun RandomAccessTextReader.tryActuallyReadNumber(
    resultBuffer: ReadNumberResult,
    allowSpecialFp: Boolean
): ZeroJsonElement? {
    val begin = position
    val errHolder = errorContainer.prepare<NumberParsingError>()

    readJsonNumber(resultBuffer, allowSpecialFp, errHolder)

    errHolder.consumeError { error ->
        position = begin
        when (error) {
            NumberParsingError.MalformedNumber, NumberParsingError.FloatOverflow -> return null
            NumberParsingError.IntegerOverflow -> {
                val uLong = readJsonUnsingedLong(onFail = errorContainer.prepare())
                return if (errorContainer.consumeError() == null) uLong else null
            }
        }
    }

    return if (resultBuffer.isDouble) {
        resultBuffer.asDouble
    } else {
        val asLong = resultBuffer.asLong
        val asInt = asLong.toInt()
        if (asInt.toLong() == asLong) asInt else asLong
    }
}

internal fun RandomAccessTextReader.readJsonNumber(
    resultBuffer: ReadNumberResult,
    allowSpecialFp: Boolean,
    onFail: ErrorContainer<NumberParsingError>
) {
    readNumber(
        resultBuffer.clear(),
        allowSpecialFp = allowSpecialFp,
        onFail = onFail,
        charClasses = JsonCharClasses.mapper,
        terminatorClass = JsonCharClasses.STR_TERM
    )
}

internal fun RandomAccessTextReader.readJsonUnsingedLong(
    onFail: DecodingErrorHandler<IntegerParsingError> = fail
): ULong = readUnsignedLong(
    charClasses = JsonCharClasses.mapper,
    terminatorClass = JsonCharClasses.STR_TERM,
    onFail = onFail
)