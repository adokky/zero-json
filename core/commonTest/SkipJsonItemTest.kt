package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SkipJsonItemTest: AbstractJsonReaderTest() {
    @Test
    fun skipToNextItem_1() {
        test("1,2,3") {
            assertTrue(skipToNextItem())
            assertEquals(2, readInt())
            assertTrue(skipToNextItem())
            assertEquals(3, readInt())
            assertUnexpectedEof()
        }
    }

    @Test
    fun skipToNextItem_1_no_eof() {
        test("1,2,3") {
            assertTrue(skipToNextItem())
            assertEquals(2, readInt())
            assertTrue(skipToNextItem())
            assertUnexpectedEof()
        }
    }

    @Test
    fun skipToNextItem_2() {
        test("[ 1 , [\"{inner}\", [{}], [],], 7495 ] ") {
            expectBeginArray()
            skipToNextItem()
            skipToNextItem()
            assertEquals(7495, readInt())
            assertFalse(skipToNextItem())
            assertFalse(skipToNextItem())
            expectEndArray()
            expectEof()
            assertUnexpectedEof()
        }
    }

    @Test
    fun skipToNextItem_3() {
        test("""{"key":{"k":"v","k":"v"},"target"}""") {
            expectBeginObject()
            skipToNextItem()
            assertEquals("target", readString())
            expectEndObject()
            expectEof()
            assertUnexpectedEof()
        }
    }

    @Test
    fun skipToNextItem_4() {
        test("""["str,in,g\",",[1,2,[3,4]],]""") {
            expectBeginArray()
            assertTrue(skipToNextItem())
            expectNextIs('[')
            assertFalse(skipToNextItem())
            expectEndArray()
            expectEof()
            assertUnexpectedEof()
        }
    }

    private fun assertUnexpectedEof() {
        assertFailsWithMessage<SerializationException>("unexpected EOF") {
            reader.skipToNextItem()
        }
    }
}

