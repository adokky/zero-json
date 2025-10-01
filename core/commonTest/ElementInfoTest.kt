@file:Suppress("OPT_IN_USAGE")
@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson

import dev.dokky.zerojson.internal.ElementInfo
import karamel.utils.enrichMessageOf
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.encoding.CompositeDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ElementInfoTest {
    @Test
    fun element_info_values() {
        val max = ZeroJsonConfiguration.MAX_PROPERTY_ELEMENT_INDEX

        for (index in listOf(10, 0xff, 0x100, max))
        for (inlineSiteIndex in listOf(-1, 0, 1, 3, max - 1, max))
        for (index in (index - 1000).coerceAtLeast(0)..index)
        {
            enrichMessageOf<Throwable>({"$index, $inlineSiteIndex"}) {
                val info = ElementInfo(index, inlineSiteIndex)

                assertEquals(index, info.index)
                assertEquals(inlineSiteIndex, info.inlineSiteIndex)

                assertEquals(inlineSiteIndex >= 0, info.isJsonInlined)
                assertEquals(inlineSiteIndex < 0, info.isRegular)
                assertTrue(info.isValid)
                assertFalse(info.isUnknown)
            }
        }
    }

    @Test
    fun special_values() {
        for (value in -1 downTo -100) {
            val info = ElementInfo(value)
            assertEquals(value, info.asInt)
            assertFalse(info.isValid)
            assertFalse(info.isRegular)
            assertFalse(info.isJsonInlined)
            assertEquals(value == CompositeDecoder.UNKNOWN_NAME, info.isUnknown)
        }
    }
}