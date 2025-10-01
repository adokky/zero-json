package dev.dokky.zerojson.framework

import io.kodec.text.DefaultCharClasses
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class DefaultJsonComposerTest {
    private val element = jsonObject {
        "key" array {
            add(1)
            addJsonArray {}
            addJsonObject {}
            add("quoted string")
            add("unquoted")
        }
    }

    @Test
    fun default() {
        assertEquals(element.toString(), element.format())
    }

    @Test
    fun unquoted() {
        assertEquals(
            """{key:[1,[],{},"quoted string",unquoted]}""",
            element.format(unquoted = true)
        )
    }

    @Test
    fun maxRandomSpaces() {
        val maxSpaces = 3
        var spaced = false

        repeat(100) {
            val json = element.format(maxRandomSpaces = maxSpaces)

            var spaces = 0
            for (c in json) {
                if (DefaultCharClasses.isWhitespace(c.code)) {
                    spaces++
                    spaced = true
                    if (spaces > maxSpaces) fail("added more than $maxSpaces spaces: ${json.normalizeWhitespaces()}")
                } else {
                    spaces = 0
                }
            }
        }

        assertTrue(spaced, "no spaces added")
    }

    private fun String.normalizeWhitespaces(): String = replace('\t', ' ').replace('\n', ' ')

    @Test
    fun corruption() {
        val n = 100
        val initial = element.toString()

        repeat(n) {
            if (element.format(corruptionInvertedProb = 1) != initial) return
        }

        fail("JSON remained intact $n times")
    }

    @Test
    fun corruption_rate() {
        val n = 40_000
        val corruptionInvertedProb = 100
        val confidenceIntervalMult = 0.6 // interval = average ± (confidenceIntervalMult * average)

        val jsonTokens = arrayOf('{', '}', '[', ']', '"', ',', ':')
        val original = element.toString()
        val tokens = (original.count { it in jsonTokens } * 1.1).roundToInt()

        var corrupted = 0
        repeat(n) {
            val formatted = element.format(
                corruptionInvertedProb = corruptionInvertedProb,
                allowCorruptQuotes = true
            )
            if (formatted != original) corrupted++
        }

        val avg = n / corruptionInvertedProb * tokens
        val max = (avg * (1 + confidenceIntervalMult)).roundToInt().coerceAtMost(n)
        val min = (avg * (1 - confidenceIntervalMult)).roundToInt()

        assertTrue(
            corrupted in min..max,
            "unexpected corruption rate: $corrupted / $n"
        )
    }
}