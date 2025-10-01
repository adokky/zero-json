package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonTreeDecodingStack
import karamel.utils.MapEntry
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonTreeDecoderStackTest {
    @Test
    fun test() {
        val descriptor = DescriptorCache(ZeroJsonConfiguration.Default).getOrCreate(serialDescriptor<CompoundDataClass>())

        val stack = JsonTreeDecodingStack(ZeroJsonConfiguration.Default)
        assertEquals(0, stack.currentMapSize)

        stack.enter(descriptor)

        assertEquals(0, stack.currentMapSize)
        assertNull(stack.removeMapEntry())

        val el1 = JsonPrimitive("e1")
        val el2 = JsonPrimitive("e2")

        assertNull(stack.getElementValue(0))
        assertNull(stack.getElementValue(1))
        stack.putElement(0, MapEntry("pk1", el1))
        stack.putElement(1, MapEntry("pk2", el2))
        assertEquals(el1, stack.getElementValue(0))
        assertEquals(el2, stack.getElementValue(1))

        val e1 = MapEntry("k1", JsonNull)
        val e2 = MapEntry("k2", JsonPrimitive("v2"))

        stack.addMapEntry(e1)
        assertEquals(1, stack.currentMapSize)
        stack.addMapEntry(e2)
        assertEquals(2, stack.currentMapSize)

        assertEquals(e2, stack.removeMapEntry())
        assertEquals(1, stack.currentMapSize)

        stack.enter(descriptor)

        assertNull(stack.getElementValue(0))
        assertNull(stack.getElementValue(1))
        assertEquals(0, stack.currentMapSize)
        assertNull(stack.removeMapEntry())

        stack.addMapEntry(e1)
        assertEquals(1, stack.currentMapSize)
        stack.addMapEntry(e2)
        assertEquals(2, stack.currentMapSize)

        assertEquals(e2, stack.removeMapEntry())
        assertEquals(1, stack.currentMapSize)
        assertEquals(e1, stack.removeMapEntry())
        assertEquals(0, stack.currentMapSize)
        assertNull(stack.removeMapEntry())
        assertEquals(0, stack.currentMapSize)

        stack.leave(descriptor)

        assertEquals(1, stack.currentMapSize)
        assertEquals(e1, stack.removeMapEntry())
        assertEquals(0, stack.currentMapSize)
        assertNull(stack.removeMapEntry())
        assertEquals(0, stack.currentMapSize)

        assertEquals(el1, stack.getElementValue(0))
        assertEquals(el2, stack.getElementValue(1))

        stack.enter(descriptor)
        stack.addMapEntry(e1)
        stack.addMapEntry(e2)
        stack.leave(descriptor)
        assertEquals(0, stack.currentMapSize)

        stack.enter(descriptor)
        assertNull(stack.getElementValue(0))
        assertNull(stack.getElementValue(1))
        assertEquals(0, stack.currentMapSize)
        assertNull(stack.removeMapEntry())

        stack.leave(descriptor)
    }
}