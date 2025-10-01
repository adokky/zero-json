package dev.dokky.zerojson.internal

import io.kodec.text.CharToClassMapper
import karamel.utils.AutoBitDescriptors

internal object JsonCharClasses: AutoBitDescriptors(capacity = 8) {
    val DOUBLE_QUOTES = uniqueBit()
    val FLOAT = uniqueBit()
    val DIGIT = uniqueBit() + FLOAT
    val TOKEN = uniqueBit()
    val STR_TERM = uniqueBit()
    val WHITESPACE = uniqueBit() + TOKEN + STR_TERM
    val WORD_TERM = uniqueBit() + STR_TERM
    val INVALID = uniqueBit() + STR_TERM

    val mapper = CharToClassMapper<JsonCharClasses>().apply {
        putBits(-1, WORD_TERM) // EOF

        // ASCII control characters
        for (i in 0 ..< 0x20) {
            putBits(i, INVALID)
        }

        // whitespace
        putBits(0x09, WHITESPACE) // HT
        putBits(0x0a, WHITESPACE) // LF
        putBits(0x0d, WHITESPACE) // CR
        putBits(0x20, WHITESPACE) // space

        for (c in '0'..'9') {
            putBits(c, DIGIT)
        }

        putBits('e', FLOAT)
        putBits('E', FLOAT)

        putBits('N', FLOAT)
        putBits('a', FLOAT)

        putBits(',', TOKEN + WORD_TERM)
        putBits('.', FLOAT)
        putBits('-', FLOAT)
        putBits('+', FLOAT)
        putBits(':', TOKEN + WORD_TERM)
        putBits('{', TOKEN + WORD_TERM)
        putBits('}', TOKEN + WORD_TERM)
        putBits('[', TOKEN + WORD_TERM)
        putBits(']', TOKEN + WORD_TERM)
        putBits('"', DOUBLE_QUOTES + TOKEN + WORD_TERM)
    }

    fun isToken(codePoint: Int): Boolean = TOKEN in mapper.getBits(codePoint)
    fun isWhitespace(codePoint: Int): Boolean = WHITESPACE in mapper.getBits(codePoint)
    fun isStringTerminator(codePoint: Int): Boolean = STR_TERM in mapper.getBits(codePoint)
}