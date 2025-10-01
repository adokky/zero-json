package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonTextWriter
import io.kodec.text.StringTextWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonTextWriterTest {
    private val writer = JsonTextWriter(StringTextWriter())

    private fun check(expected: String, write: JsonTextWriter.() -> Unit) {
        writer.textWriter = StringTextWriter()
        writer.write()
        assertEquals(expected, writer.textWriter.toString())
    }

    @Test
    fun simple() {
        check("0.0") { writeNumber(0.0) }
        check("0.0") { writeNumber(0.0f) }
        check("-42") { writeNumber(-42L) }
        check("-42") { writeNumber(-42) }
        check("-42") { writeNumber((-42).toShort()) }
        check("-42") { writeNumber((-42).toByte()) }
        check("\"Привет!\"") { writeString("Привет!") }
        check("\"Ё\"") { writeString('Ё') }
    }

    private val escapeCharacters = mapOf(
        '"' to "\\\"",
        '\n' to "\\n",
        '\r' to "\\r",
        '\t' to "\\t",
        '\b' to "\\b",
        '\\' to "\\\\",
        Char(0x0c) to "\\f",
        '\u2028' to "\\u2028",
        '\u2029' to "\\u2029"
    )

    @Test
    fun simple_escaping() {
        fun check(expected: String, char: Char) {
            check(expected) { writeString(char) }
            check(expected) { writeString(char.toString()) }
        }

        check(""""\""""", '"')
        check(""""\\"""", '\\')
        check(""""\t"""", '\t')
        check(""""\n"""", '\n')
        check(""""\r"""", '\r')
        check(""""\b"""", '\b')
        check(""""\f"""", Char(0x0c))
        check(""""\u2028"""", '\u2028')
        check(""""\u2029"""", '\u2029')

        repeat(0x20) { code ->
            if (code.toChar() in escapeCharacters) return@repeat

            val expected = buildString(8) {
                append("\"\\u")
                val s = code.toString(radix = 16)
                repeat(4 - s.length) { append('0') }
                append(s)
                append('\"')
            }
            check(expected, Char(code))
        }

        // check all other characters not escaped
        for (code in 0x20.toChar()..0xffff.toChar()) {
            if (code in escapeCharacters) continue
            check("\"$code\"", code)
        }
    }

    @Test
    fun nested_escaping() {
        fun check(expected: String, char: Char) {
            var escaped = expected
            for (level in 0..4) {
                check(escaped) {
                    repeat(level) { beginString() }
                    writeString(char)
                    repeat(level) { endString() }
                }
                check(escaped) {
                    repeat(level) { beginString() }
                    writeString(char.toString())
                    repeat(level) { endString() }
                }
                escaped = buildString {
                    append('"')
                    for (c in escaped) {
                        append(escapeCharacters[c] ?: c)
                    }
                    append('"')
                }
            }
        }

        check(""""\""""", '"')
        check(""""\\"""", '\\')
        check(""""\t"""", '\t')
        check(""""\n"""", '\n')
        check(""""\r"""", '\r')
        check(""""\b"""", '\b')
        check(""""\f"""", Char(0x0c))
        check(""""\u2028"""", '\u2028')
        check(""""\u2029"""", '\u2029')

        // check all other characters not escaped
        for (code in 0x20.toChar()..0xffff.toChar()) {
            if (code in escapeCharacters) continue
            check("\"$code\"", code)
        }
    }
}