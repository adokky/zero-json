package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonTextDecodingStack
import kotlinx.serialization.descriptors.serialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestDecoderStackTest {
    private val cache = DescriptorCache(ZeroJsonConfiguration.Default)
    private val jsonDescriptor = cache.getOrCreate(serialDescriptor<CompoundDataClass>())

    @Test
    fun simple() {
        val stack = JsonTextDecodingStack(capacity = 10, maxInlinedElements = 100)
        assertEquals(0, stack.depth)

        stack.enter(jsonDescriptor)
        assertEquals(1, stack.depth)
        assertEquals(-1, stack.elementIndex)
        assertFalse(stack.isInlined)
        assertFalse(stack.isInlineRoot)
        assertFalse(stack.isInlinedOrInlineRoot)
        assertFalse(stack.isDecodingNull)
        assertFalse(stack.shouldSkipDiscriminator)
        assertEquals(0, stack.currentKeyStart)

        stack.isDecodingNull = true
        stack.shouldSkipDiscriminator = true
        stack.currentKeyStart = 53543

        assertTrue(stack.isDecodingNull)
        assertTrue(stack.shouldSkipDiscriminator)
        assertEquals(53543, stack.currentKeyStart)

        stack.elementIndex = -3
        assertEquals(-3, stack.elementIndex)

        stack.elementIndex = 167
        assertEquals(167, stack.elementIndex)

        stack.elementIndex = -9000
        assertEquals(-9000, stack.elementIndex)

        stack.testSimpleFrame(expectedDepth = 2)

        assertEquals(-9000, stack.elementIndex)
        assertFalse(stack.isInlined)
        assertFalse(stack.isInlineRoot)
        assertFalse(stack.isInlinedOrInlineRoot)
        assertTrue(stack.isDecodingNull)
        assertTrue(stack.shouldSkipDiscriminator)
        assertEquals(53543, stack.currentKeyStart)

        stack.leave(jsonDescriptor.serialDescriptor)
        assertEquals(0, stack.depth)
    }

    private fun JsonTextDecodingStack.testSimpleFrame(expectedDepth: Int) {
        enter(jsonDescriptor)
        assertEquals(expectedDepth, depth)
        assertEquals(-1, elementIndex)
        assertFalse(isInlined)
        assertFalse(isInlineRoot)
        assertFalse(isInlinedOrInlineRoot)
        assertFalse(isDecodingNull)
        assertFalse(shouldSkipDiscriminator)
        assertEquals(0, currentKeyStart)

        elementIndex = -5
        assertEquals(-5, elementIndex)

        leave(jsonDescriptor.serialDescriptor)
        assertEquals(expectedDepth - 1, depth)
    }

    @Test
    fun polymorphic_frames() {
        val stack = JsonTextDecodingStack(capacity = 10, maxInlinedElements = 100)
        assertEquals(0, stack.depth)

        stack.enterPolymorphic(jsonDescriptor, discriminatorKeyStart = 123, discriminatorValueEnd = 234, shouldSkipDiscriminator = true)
        assertEquals(1, stack.depth)
        assertEquals(-1, stack.elementIndex)
        assertFalse(stack.isInlined)
        assertFalse(stack.isInlineRoot)
        assertFalse(stack.isInlinedOrInlineRoot)
        assertFalse(stack.isDecodingNull)
        assertTrue(stack.shouldSkipDiscriminator)
        assertEquals(0, stack.currentKeyStart)
        assertEquals(123, stack.discriminatorKeyStart)
        assertEquals(234, stack.discriminatorValueEnd)

        stack.elementIndex = 0
        assertEquals(0, stack.elementIndex)

        stack.currentKeyStart = 98765
        assertEquals(98765, stack.currentKeyStart)

        stack.testSimpleFrame(expectedDepth = 2)

        assertEquals(0, stack.elementIndex)
        assertFalse(stack.isInlined)
        assertFalse(stack.isInlineRoot)
        assertFalse(stack.isInlinedOrInlineRoot)
        assertFalse(stack.isDecodingNull)
        assertTrue(stack.shouldSkipDiscriminator)
        assertEquals(98765, stack.currentKeyStart)
        assertEquals(123, stack.discriminatorKeyStart)
        assertEquals(234, stack.discriminatorValueEnd)

        stack.leave(jsonDescriptor.serialDescriptor)
        assertEquals(0, stack.depth)
    }
}