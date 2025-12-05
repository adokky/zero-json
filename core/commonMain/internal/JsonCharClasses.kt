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
        assignClasses(-1, WORD_TERM) // EOF

        // ASCII control characters
        for (i in 0 ..< 0x20) {
            assignClasses(i, INVALID)
        }

        // whitespace
        assignClasses(0x09, WHITESPACE) // HT
        assignClasses(0x0a, WHITESPACE) // LF
        assignClasses(0x0d, WHITESPACE) // CR
        assignClasses(0x20, WHITESPACE) // space

        for (c in '0'..'9') {
            assignClasses(c, DIGIT)
        }

        assignClasses('e', FLOAT)
        assignClasses('E', FLOAT)

        assignClasses('N', FLOAT)
        assignClasses('a', FLOAT)

        assignClasses(',', TOKEN + WORD_TERM)
        assignClasses('.', FLOAT)
        assignClasses('-', FLOAT)
        assignClasses('+', FLOAT)
        assignClasses(':', TOKEN + WORD_TERM)
        assignClasses('{', TOKEN + WORD_TERM)
        assignClasses('}', TOKEN + WORD_TERM)
        assignClasses('[', TOKEN + WORD_TERM)
        assignClasses(']', TOKEN + WORD_TERM)
        assignClasses('"', DOUBLE_QUOTES + TOKEN + WORD_TERM)
    }

    fun isToken(codePoint: Int): Boolean = TOKEN in mapper.getClasses(codePoint)
    fun isWhitespace(codePoint: Int): Boolean = WHITESPACE in mapper.getClasses(codePoint)
    fun isStringTerminator(codePoint: Int): Boolean = STR_TERM in mapper.getClasses(codePoint)
}