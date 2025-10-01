package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import dev.dokky.zerojson.framework.GlobalTestMode
import dev.dokky.zerojson.framework.TestMode
import io.kodec.buffers.ArrayDataBuffer
import io.kodec.buffers.getStringAscii
import kotlinx.serialization.SerializationException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

class JunkFuzzer: AbstractDecoderTest() {
    @Test
    fun fixed_junk() {
        for (fixture in arrayOf(
            "[]",
            "{%",
            "{\":]\\ \"�:["
        )) {
            check(fixture.encodeToArrayBuffer(), chars = fixture.length)
        }
    }

    @Test
    fun random_junk() {
        val maxStringLength = 70
        val buffer = ArrayDataBuffer(maxStringLength)

        val iterations = when(GlobalTestMode) {
            TestMode.QUICK -> 10_000
            TestMode.DEFAULT -> 1_000_000
            TestMode.FULL -> 10_000_000
        }

        repeat(iterations) {
            val length = Random.nextInt(maxStringLength + 1)
            repeat(length) { pos ->
                buffer[pos] = when (Random.nextInt(120)) {
                    in 0..10 -> '{'.code
                    in 10..20 -> '}'.code
                    in 20..30 -> '['.code
                    in 30..40 -> ']'.code
                    in 40..50 -> ','.code
                    in 50..60 -> '"'.code
                    in 60..65 -> '\\'.code
                    in 65..75 -> ':'.code
                    in 75..90 -> ' '.code
                    in 116..120 -> Random.nextInt()
                    else -> Random.nextInt(33, 'я'.code + 1)
                }
            }

            check(buffer.subBuffer(0, length), length)
        }
    }

    private fun check(input: ArrayDataBuffer, chars: Int) {
        try {
            zjson.decode<ComplexClass>(input)
        } catch (e: Throwable) {
            if (e !is SerializationException && e !is ZeroJsonDecodingException)
                throw AssertionError(input.getStringAscii(0, chars), e)

            if (e is JsonMaxDepthReachedException) {
                var maxNesting = 0
                for (byte in input) {
                    if (byte == '{'.code || byte == '['.code) maxNesting++
                }
                if (maxNesting < zjson.configuration.maxStructureDepth) {
                    fail(
                        "JsonMaxDepthReachedException is thrown, but actual nesting level is $maxNesting. " +
                        "String:\n${input.getStringAscii(0, chars)}"
                    )
                }
            }
        }
    }
}