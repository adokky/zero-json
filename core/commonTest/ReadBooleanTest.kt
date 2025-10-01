package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadBooleanTest: AbstractJsonReaderTest() {
    @Test
    fun happy_path() {
        test("true") { assertTrue(reader.readBoolean()) }
        test("false") { assertFalse(reader.readBoolean()) }

        test("true ") {
            assertTrue(reader.readBoolean())
            reader.expectEof() // expect whitespace to be skipped
        }
        test("false ") {
            assertFalse(reader.readBoolean())
            reader.expectEof() // expect whitespace to be skipped
        }

        test("true:") { assertTrue(reader.readBoolean()) }
        test("false:") { assertFalse(reader.readBoolean()) }
    }

    @Test
    fun errors() {
        fun assertFails(input: String) {
            test(input) {
                assertFailsWithMessage<ZeroJsonDecodingException>("expected 'true' or 'false'") {
                    reader.readBoolean()
                }
            }
            test(input) {
                assertNull(reader.tryReadBoolean())
            }
        }

        arrayOf("True", "False", "T", "F", " ", "true5", "falsetrue", "false.")
            .forEach(::assertFails)
    }
}