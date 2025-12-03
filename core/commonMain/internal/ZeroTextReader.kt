package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.text.StringTextReader
import io.kodec.text.TextReader
import io.kodec.text.Utf8TextReader

internal interface ZeroTextReader: TextReader

internal class ZeroUtf8TextReader(
    private val errorMessageBuilder: StringBuilderWrapper = StringBuilderWrapper()
): Utf8TextReader(), ZeroTextReader {
    override fun fail(msg: String): Nothing =
        throw ZeroJsonDecodingException(msg, position = position, path = null)

    override fun expectNextIs(code: Int) {
        if (nextCodePoint != code) unexpectedNextChar(errorMessageBuilder, expected = code)
    }
}

internal class ZeroStringTextReader(
    private val errorMessageBuilder: StringBuilderWrapper = StringBuilderWrapper()
) : StringTextReader(), ZeroTextReader {
    override fun fail(msg: String): Nothing =
        throw ZeroJsonDecodingException(msg, position = position, path = null)

    override fun expectNextIs(code: Int) {
        if (nextCodePoint != code) unexpectedNextChar(errorMessageBuilder, expected = code)
    }
}