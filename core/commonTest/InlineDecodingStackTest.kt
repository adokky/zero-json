@file:Suppress("OPT_IN_USAGE")
@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson

import dev.dokky.zerojson.internal.InlineRootStack
import dev.dokky.zerojson.internal.ZeroJsonDescriptor
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.serialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InlineDecodingStackTest {
    @Test
    fun element_position_encoding() {
        for (keyOffset in arrayOf(0, 1, 45, InlineRootStack.MAX_ELEMENT_KEY_OFFSET))
        for (pos in arrayOf(0, 1, 45, InlineRootStack.MAX_ELEMENT_VALUE_POSITION)) {
            val encoded = InlineRootStack.encodeElementInfo(pos, keyOffset)
            assertEquals(pos, InlineRootStack.elementValuePosition(encoded))
            assertEquals(keyOffset, InlineRootStack.elementKeyOffset(encoded))
        }
    }

    @Test
    fun if_key_offset_is_to_large_it_should_be_zeroed() {
        InlineRootStack.encodeElementInfo(
            valuePosition = InlineRootStack.MAX_ELEMENT_VALUE_POSITION,
            keyOffset = InlineRootStack.MAX_ELEMENT_KEY_OFFSET + 1
        ).let { encoded ->
            assertEquals(0, InlineRootStack.elementKeyOffset(encoded))
            assertEquals(InlineRootStack.MAX_ELEMENT_VALUE_POSITION, InlineRootStack.elementValuePosition(encoded))
        }

        InlineRootStack.encodeElementInfo(
            valuePosition = 0,
            keyOffset = InlineRootStack.MAX_ELEMENT_KEY_OFFSET + 1
        ).let { encoded ->
            assertEquals(0, InlineRootStack.elementKeyOffset(encoded))
            assertEquals(0, InlineRootStack.elementValuePosition(encoded))
        }
    }

    @Serializable
    private data class Sample(@JsonInline val inlined: SimpleDataClass)

    private val cache = DescriptorCache(ZeroJsonConfiguration.Default)
    private val desc = cache.getOrCreate(serialDescriptor<Sample>())

    @Test
    fun simple_scenario() {
        val stack = InlineRootStack(maxDepth = 2, maxTotalElements = 12)

        assertNull(stack.descriptorOrNull())

        stack.enter(desc, decoderStackDepth = Int.MAX_VALUE - 7)
        assertInitialStackFrame(stack, desc)

        stack.setInlineRootEndPosition(Int.MAX_VALUE - 55)
        assertEquals(Int.MAX_VALUE - 55, stack.getInlineRootEndPosition())
        assertEquals(Int.MAX_VALUE - 7, stack.getDecoderStackDepth())
        assertEquals(0, stack.getElementPosition(elementIndex = 0))

        stack.markElementInline(elementIndex = 0)
        assertEquals(1, stack.getElementPosition(elementIndex = 0))

        stack.setElementPosition(elementIndex = 0, valuePosition = 111, keyOffset = 67)
        assertEquals(InlineRootStack.encodeElementInfo(111, 67), stack.getElementPosition(elementIndex = 0))

        stack.leave(desc.serialDescriptorUnsafe)
        assertNull(stack.descriptorOrNull())

        stack.enter(desc, decoderStackDepth = Int.MAX_VALUE - 7)
        assertInitialStackFrame(stack, desc)

        stack.enter(desc, decoderStackDepth = Int.MAX_VALUE - 7)
        assertInitialStackFrame(stack, desc)

        stack.leave(desc.serialDescriptorUnsafe)
        assertNotNull(stack.descriptorOrNull())
        assertInitialStackFrame(stack, desc)

        stack.leave(desc.serialDescriptorUnsafe)
        assertNull(stack.descriptorOrNull())
    }

    private fun assertInitialStackFrame(stack: InlineRootStack, desc: ZeroJsonDescriptor) {
        assertEquals(Int.MAX_VALUE - 7, stack.getDecoderStackDepth())
        assertEquals(desc, stack.descriptor())
        assertEquals(0, stack.getElementPosition(elementIndex = 0))
        assertEquals(0, stack.getInlineRootEndPosition())
        assertEquals(0, stack.mapKeyPositionCount)
    }

    @Test
    fun map_key_positions() {
        val stack = InlineRootStack(maxDepth = 2, maxTotalElements = 12)
        stack.enter(desc, decoderStackDepth = 123)

        assertEquals(0, stack.mapKeyPositionCount)

        stack.addMapKeyPosition(11)
        stack.addMapKeyPosition(22)
        stack.addMapKeyPosition(33)
        assertEquals(3, stack.mapKeyPositionCount)

        assertEquals(33, stack.removeMapKeyPosition())
        assertEquals(2, stack.mapKeyPositionCount)

        assertEquals(22, stack.removeMapKeyPosition())
        assertEquals(1, stack.mapKeyPositionCount)

        stack.enter(desc, decoderStackDepth = 345)
        assertEquals(0, stack.mapKeyPositionCount)

        stack.addMapKeyPosition(44)
        stack.addMapKeyPosition(55)
        assertEquals(2, stack.mapKeyPositionCount)

        assertEquals(55, stack.removeMapKeyPosition())
        assertEquals(1, stack.mapKeyPositionCount)

        stack.addMapKeyPosition(77)
        assertEquals(2, stack.mapKeyPositionCount)

        assertEquals(77, stack.removeMapKeyPosition())
        assertEquals(1, stack.mapKeyPositionCount)

        assertEquals(44, stack.removeMapKeyPosition())
        assertEquals(0, stack.mapKeyPositionCount)

        stack.leave(desc.serialDescriptorUnsafe)
        assertEquals(1, stack.mapKeyPositionCount)

        assertEquals(11, stack.removeMapKeyPosition())
        assertEquals(0, stack.mapKeyPositionCount)
    }
}