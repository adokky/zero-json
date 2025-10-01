package dev.dokky.zerojson.internal

import io.kodec.text.RandomAccessTextReader
import io.kodec.text.TextReader

private val NULL_HASH_CODE = "null".hashCode()
private val TRUE_HASH_CODE = "true".hashCode()
private val FALSE_HASH_CODE = "false".hashCode()

private val NULL_OR_BOOL_HASH_MASK = run {
    val commonOnes  = NULL_HASH_CODE       and TRUE_HASH_CODE       and FALSE_HASH_CODE
    val commonZeros = NULL_HASH_CODE.inv() and TRUE_HASH_CODE.inv() and FALSE_HASH_CODE.inv()
    commonOnes or commonZeros
}
private val NULL_OR_BOOL_HASH_BITS = (NULL_HASH_CODE and NULL_OR_BOOL_HASH_MASK)
    .also { bits ->
        check(NULL_HASH_CODE and NULL_OR_BOOL_HASH_MASK == bits)
        check(TRUE_HASH_CODE and NULL_OR_BOOL_HASH_MASK == bits)
        check(FALSE_HASH_CODE and NULL_OR_BOOL_HASH_MASK == bits)

        check("null_".hashCode() and NULL_OR_BOOL_HASH_MASK != bits)
        check("".hashCode() and NULL_OR_BOOL_HASH_MASK != bits)
        check("zz".hashCode() and NULL_OR_BOOL_HASH_MASK != bits)
    }

internal fun checkUnquotedString(
    reader: TextReader,
    input: StringBuilder,
    start: Int,
    hash: Int,
    allowNull: Boolean,
    allowBoolean: Boolean
) {
    val length = input.length - start

    if (length == 0) reader.throwExpectedString()

    if (hash and NULL_OR_BOOL_HASH_MASK != NULL_OR_BOOL_HASH_BITS) return

    if (!allowBoolean) {
        when(hash) {
            TRUE_HASH_CODE -> if (length == 4) checkStringNotTrue(reader, input, start)
            FALSE_HASH_CODE -> if (length == 5) checkStringNotFalse(reader, input, start)
        }
    }

    if (!allowNull) {
        if (hash == NULL_HASH_CODE && length == 4) checkStringNotNull(reader, input, start)
    }
}

internal fun checkUnquotedString(
    reader: RandomAccessTextReader,
    start: Int,
    hash: Int,
    allowNull: Boolean,
    allowBoolean: Boolean
) {
    val length = reader.position - start

    if (length == 0) reader.throwExpectedString()

    if (hash and NULL_OR_BOOL_HASH_MASK != NULL_OR_BOOL_HASH_BITS) return

    if (!allowBoolean) {
        when(hash) {
            TRUE_HASH_CODE -> if (length == 4) reader.checkStringNotTrue(start)
            FALSE_HASH_CODE -> if (length == 5) reader.checkStringNotFalse(start)
        }
    }

    if (!allowNull) {
        if (hash == NULL_HASH_CODE && length == 4) reader.checkStringNotNull(start)
    }
}

private fun checkStringNotNull(reader: TextReader, input: StringBuilder, start: Int) {
    if (input[start    ] == 'n' &&
        input[start + 1] == 'u' &&
        input[start + 2] == 'l' &&
        input[start + 3] == 'l')
    {
        reader.throwExpectedStringGotNull()
    }
}

private fun checkStringNotTrue(reader: TextReader, input: StringBuilder, start: Int) {
    if (input[start    ] == 't' &&
        input[start + 1] == 'r' &&
        input[start + 2] == 'u' &&
        input[start + 3] == 'e')
    {
        reader.throwExpectedStringGotBool()
    }
}

private fun checkStringNotFalse(reader: TextReader, input: StringBuilder, start: Int) {
    if (input[start    ] == 'f' &&
        input[start + 1] == 'a' &&
        input[start + 2] == 'l' &&
        input[start + 3] == 's' &&
        input[start + 4] == 'e')
    {
        reader.throwExpectedStringGotBool()
    }
}

private fun RandomAccessTextReader.checkStringNotNull(start: Int) {
    val end = position

    val c1 = readAsciiCode(end - 4)
    val c2 = readAsciiCode(end - 3)
    val c3 = readAsciiCode(end - 2)
    val c4 = readAsciiCode(end - 1)

    if (c1 == 'n'.code &&
        c2 == 'u'.code &&
        c3 == 'l'.code &&
        c4 == 'l'.code)
    {
        throwExpectedStringGotNull()
    }
}

private fun RandomAccessTextReader.checkStringNotTrue(start: Int) {
    val end = position

    val c1 = readAsciiCode(end - 4)
    val c2 = readAsciiCode(end - 3)
    val c3 = readAsciiCode(end - 2)
    val c4 = readAsciiCode(end - 1)

    if (c1 == 't'.code &&
        c2 == 'r'.code &&
        c3 == 'u'.code &&
        c4 == 'e'.code)
    {
        throwExpectedStringGotBool()
    }
}

private fun RandomAccessTextReader.checkStringNotFalse(start: Int) {
    val end = position

    val c1 = readAsciiCode(end - 5)
    val c2 = readAsciiCode(end - 4)
    val c3 = readAsciiCode(end - 3)
    val c4 = readAsciiCode(end - 2)
    val c5 = readAsciiCode(end - 1)

    if (c1 == 'f'.code &&
        c2 == 'a'.code &&
        c3 == 'l'.code &&
        c4 == 's'.code &&
        c5 == 'e'.code)
    {
        throwExpectedStringGotBool()
    }
}