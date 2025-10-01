package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.AbstractJsonReaderTest
import karamel.utils.enrichMessageOf
import karamel.utils.nearlyEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonNumbersTest: AbstractJsonReaderTest() {
    @Test
    fun integers() {
        val int32 = listOf(
            0, 1, 123, 123456789, Int.MAX_VALUE, Int.MIN_VALUE
        )

        for (n in int32) {
            setBufferJson(n.toString())
            assertEquals(n, reader.readInt())

            setBufferJson((-n).toString())
            assertEquals(-n, reader.readInt())
        }

        val int64 = listOf(
            Int.MAX_VALUE.toLong() + 1L, Long.MAX_VALUE - 2L, Long.MIN_VALUE
        )

        for (n in int64) {
            setBufferJson(n.toString())
            assertEquals(n, reader.readLong())

            setBufferJson((-n).toString())
            assertEquals(-n, reader.readLong())
        }
    }

    @Test
    fun floats() {
        for (n in listOf(
            0f, 1f, 123f, 0.0001f, 1.1f, 0.123f, 1.2345679E8f, 10000.0f, 0.0000000001f
        ).flatMap { listOf(it, -it) }) {
            for (nStr in arrayOf(n.toString(), "$n ,")) {
                enrichMessageOf<Throwable>({ "failed on: '$nStr'" }) {
                    setBufferJson(nStr)
                    val actual = reader.readFloat()
                    assertTrue(actual.nearlyEquals(n), "expected: $nStr, got: $actual")
                }
            }
        }
    }

    @Test
    fun doubles() {
        for (n in listOf(
            0.0, 1.0, 123.0, 0.0001, 1.1, 0.123, 1.23456792E8, 10000.00002, 0.0000000001
        )) {
            enrichMessageOf<Throwable>({ "failed on: $n" }) {
                setBufferJson((-n).toString())
                val actual = reader.readDouble()
                assertTrue(actual.nearlyEquals(-n), "expected: ${-n}, got:$actual ")
            }
        }
    }
}