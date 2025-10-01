package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.readJsonString
import dev.dokky.zerojson.internal.skipString
import io.kodec.DecodingErrorHandler
import kotlin.test.assertEquals
import kotlin.test.fail

class SkipJsonStringTest: AbstractJsonStringTest<String>() {
    override fun JsonReaderImpl.readTestString(
        requireQuotes: Boolean,
        maxLength: Int,
        onMaxLength: DecodingErrorHandler<Any>,
        allowNull: Boolean,
        allowBoolean: Boolean
    ): String {
        val start = position
        skipString(
            expectQuotes = requireQuotes,
            maxLength = maxLength,
            onMaxLength = onMaxLength,
            allowNull = allowNull,
            allowBoolean = allowBoolean
        )
        val end = position

        position = start
        val result = try {
            buildString {
                input.readJsonString(
                    this,
                    requireQuotes = requireQuotes,
                    maxLength = maxLength,
                    onMaxLength = onMaxLength,
                    allowNull = allowNull,
                    allowBoolean = allowBoolean,
                )
            }
        } catch (e: Throwable) {
            fail("readJsonString() failed", e)
        }
        position = end
        return result
    }

    override fun checkResult(original: String, result: String) {
        assertEquals(original, result)
    }
}