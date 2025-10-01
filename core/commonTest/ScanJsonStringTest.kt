package dev.dokky.zerojson

import dev.dokky.zerojson.framework.shouldBeEscaped
import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.ScanResult
import dev.dokky.zerojson.internal.scanString
import io.kodec.DecodingErrorHandler
import io.kodec.StringsUTF16
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ScanJsonStringTest: AbstractJsonStringTest<ScanResult>() {
    override fun JsonReaderImpl.readTestString(
        requireQuotes: Boolean,
        maxLength: Int,
        onMaxLength: DecodingErrorHandler<Any>,
        allowNull: Boolean,
        allowBoolean: Boolean
    ): ScanResult {
        return scanString(
            requireQuotes = requireQuotes,
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowNull = allowNull,
            allowBoolean = allowBoolean,
            allowEscapes = true
        )
    }

    override fun checkResult(original: String, result: ScanResult) {
        assertEquals(original.shouldBeEscaped(), result.isEscaped)
        assertEquals(StringsUTF16.countCodePoints(original), result.codePoints)
        assertEquals(original.hashCode(), result.hash)
    }

    @Test
    fun scan_result() {
        for (codePoints in intArrayOf(0, 1, 2, 3, 123, Int.MAX_VALUE shr 1))
            for (hashCode in intArrayOf(Int.MIN_VALUE, -123, -3, -1, 0, 1, 2, 3, 123, Int.MAX_VALUE)) {
                fun check(r: ScanResult, quoted: Int) {
                    assertEquals(codePoints, r.codePoints)
                    assertEquals(hashCode, r.hash)
                    assertEquals(quoted, r.quoted)
                    assertFalse(r.isEscaped)
                }

                val result = ScanResult(codePoints, hashCode)

                check(result, quoted = 0)
                check(result.markQuoted(), quoted = 1)
            }

        ScanResult.EscapedString.also {
            assertEquals(0, it.codePoints)
            assertEquals(0, it.hash)
            assertEquals(0, it.quoted)
            assertTrue(it.isEscaped)
        }
    }
}