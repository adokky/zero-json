package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithMessage
import dev.dokky.zerojson.internal.*
import io.kodec.buffers.ArrayBuffer
import io.kodec.text.asUtf8SubString
import io.kodec.text.substringWrapper
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.json.JsonNames
import kotlin.test.*

class ZeroJsonDescriptorTest {
    @Serializable
    private data class Parent(val string: String, @JsonInline val inline: Child, val int: Int)

    @Serializable
    private data class Child(val name: String, val age: Int)

    @Test
    fun with_json_inline() {
        val serialDesc = serialDescriptor<Parent>()
        val cache = DescriptorCache(ZeroJsonConfiguration.Default)
        val desc = cache.getOrCreateUnsafe(serialDesc)

        checkSerialDescriptor(serialDesc, desc)
        assertFalse(desc.ignoreUnknownKeys)
        assertFalse(desc.caseInsensitiveEnum)
        assertTrue(desc.allNamesAreAscii)
        assertTrue(desc.hasJsonInlineElements)
        assertFalse(desc.hasInlineMapElement)
        assertTrue(desc.inlineMapElement.asInt < 0)
        assertNull(desc.classDiscriminator)
        assertEquals(5, desc.totalElementCount)

        assertEquals(ElementInfo.UNKNOWN_NAME, desc.getElementInfoByName("string,"))
        assertEquals(ElementInfo.UNKNOWN_NAME, desc.getElementInfoByName("age,"))

        desc.checkElement(ElementInfo(index = 0, inlineSiteIndex = -1), "string")
        desc.checkElement(ElementInfo.UNKNOWN_NAME,                                  "inline")
        desc.checkElement(ElementInfo(index = 2, inlineSiteIndex = -1), "int")

        desc.checkElement(ElementInfo(index = 3, inlineSiteIndex = 1), "name")
        desc.checkElement(ElementInfo(index = 4, inlineSiteIndex = 1), "age")

        for (index in 0..4) {
            assertEquals(if (index == 1) 3 else 0, desc.getChildElementsOffset(elementAbsoluteIndex = index))
        }

        assertEquals(0, desc.getChildElementsOffset(0 + 0))
        assertEquals(3, desc.getChildElementsOffset(0 + 1))
        assertEquals(0, desc.getChildElementsOffset(0 + 2))
        assertEquals(0, desc.getChildElementsOffset(3 + 0))
        assertEquals(0, desc.getChildElementsOffset(3 + 1))

        for (index in 0..4) {
            assertEquals(index == 1, desc.isElementJsonInline(index))
        }

        for (index in 0..2) {
            desc.checkElementDescriptor(cache, index)
        }
    }

    @Suppress("unused")
    @Serializable
    private enum class EnumTest {
        Entry1, Entry2, @JsonNames("e3", "et3") Entry3
    }

    @Test
    fun enum_test() {
        val serialDesc = serialDescriptor<EnumTest>()
        val cache = DescriptorCache(ZeroJsonConfiguration.Default)
        val desc = cache.getOrCreateUnsafe(serialDesc)

        checkSerialDescriptor(serialDesc, desc)
        assertFalse(desc.ignoreUnknownKeys)
        assertFalse(desc.caseInsensitiveEnum)
        assertTrue(desc.allNamesAreAscii)
        assertFalse(desc.hasJsonInlineElements)
        assertFalse(desc.hasInlineMapElement)
        assertTrue(desc.inlineMapElement.asInt < 0)
        assertNull(desc.classDiscriminator)
        assertEquals(3, desc.totalElementCount)

        assertEquals(ElementInfo.UNKNOWN_NAME, desc.getElementInfoByName("entry1"))
        assertEquals(ElementInfo.UNKNOWN_NAME, desc.getElementInfoByName("ET3"))

        desc.checkElement(ElementInfo(index = 0, inlineSiteIndex = -1), "Entry1")
        desc.checkElement(ElementInfo(index = 1, inlineSiteIndex = -1), "Entry2")
        desc.checkElement(ElementInfo(index = 2, inlineSiteIndex = -1), "Entry3", "e3", "et3")
    }

    @Test
    fun enum_case_insensitive_ascii_test() {
        val serialDesc = serialDescriptor<EnumTest>()
        val cache = DescriptorCache(ZeroJsonConfiguration(decodeEnumsCaseInsensitive = true))
        val desc = cache.getOrCreateUnsafe(serialDesc)

        checkSerialDescriptor(serialDesc, desc)
        assertFalse(desc.ignoreUnknownKeys)
        assertTrue(desc.caseInsensitiveEnum)
        assertTrue(desc.allNamesAreAscii)
        assertFalse(desc.hasJsonInlineElements)
        assertFalse(desc.hasInlineMapElement)
        assertTrue(desc.inlineMapElement.asInt < 0)
        assertNull(desc.classDiscriminator)
        assertEquals(3, desc.totalElementCount)

        assertEquals(ElementInfo.UNKNOWN_NAME, desc.getElementInfoByName("entry_1"))

        desc.checkElement(ElementInfo(index = 0, inlineSiteIndex = -1), "Entry1", "enTry1")
        desc.checkElement(ElementInfo(index = 1, inlineSiteIndex = -1), "Entry2", "ENTRY2", "entry2")
        desc.checkElement(ElementInfo(index = 2, inlineSiteIndex = -1), "Entry3", "entry3", "e3", "et3", "E3", "ET3")
    }

    @Suppress("unused")
    @Serializable
    private enum class EnumTestNonAscii {
        Entry1, @JsonNames("Привет, Мир!", "et2") Entry2, Entry3
    }

    @Test
    fun enum_case_insensitive_utf_test() {
        val serialDesc = serialDescriptor<EnumTestNonAscii>()
        val cache = DescriptorCache(ZeroJsonConfiguration(decodeEnumsCaseInsensitive = true))
        val desc = cache.getOrCreateUnsafe(serialDesc)

        checkSerialDescriptor(serialDesc, desc)
        assertFalse(desc.ignoreUnknownKeys)
        assertTrue(desc.caseInsensitiveEnum)
        assertFalse(desc.allNamesAreAscii)
        assertFalse(desc.hasJsonInlineElements)
        assertFalse(desc.hasInlineMapElement)
        assertTrue(desc.inlineMapElement.asInt < 0)
        assertNull(desc.classDiscriminator)
        assertEquals(3, desc.totalElementCount)

        assertEquals(ElementInfo.UNKNOWN_NAME, desc.getElementInfoByName("entry_1"))

        desc.checkElement(ElementInfo(index = 0, inlineSiteIndex = -1), "Entry1", "enTry1")
        desc.checkElement(ElementInfo(index = 1, inlineSiteIndex = -1),
            "Entry2", "eNtrY2", "Привет, Мир!", "привет, мир!", "ET2")
        desc.checkElement(ElementInfo(index = 2, inlineSiteIndex = -1), "Entry3", "entry3")
    }

    @Suppress("unused")
    @Serializable private class CyclicInlinedElement1(
        val left: CyclicInlinedElement1?,
        @JsonInline val inlined: CyclicInlinedElement1?
    )

    @Suppress("unused")
    @Serializable private class CyclicInlinedElement2(@JsonInline val inlined: Nested?) {
        @Serializable class Nested(@JsonInline val inlined: CyclicInlinedElement2?)
    }

    @Test
    fun inline_cycle_1() {
        assertFailsWithMessage<SerializationException>("cyclic") {
            DescriptorCache(ZeroJsonConfiguration.Default).getOrCreate(
                serialDescriptor<CyclicInlinedElement1>()
            )
        }
    }

    @Test
    fun inline_cycle_2() {
        assertFailsWithMessage<SerializationException>("cyclic") {
            DescriptorCache(ZeroJsonConfiguration.Default).getOrCreate(
                serialDescriptor<CyclicInlinedElement2>()
            )
        }
    }

    private val tempBuffer = ArrayBuffer(100)

    private fun ZeroJsonDescriptor.checkElement(expectedElementInfo: ElementInfo, vararg names: String) {
        for (name in names) {
            assertEquals(expectedElementInfo, getElementInfoByName(name),
                "lookup by String '$name'")
            assertEquals(expectedElementInfo, getElementInfoByName(name.asUtf8SubString(), tempBuffer),
                "lookup by SeekableTextReaderSubString '$name'")
            assertEquals(expectedElementInfo, getElementInfoByName(name.substringWrapper(), tempBuffer),
                "lookup by SimpleSubString '$name'")
        }

        if (expectedElementInfo.isRegular) {
            assertEquals(names.first(), getElementName(expectedElementInfo.index))
        }
    }

    private fun checkSerialDescriptor(
        serialDesc: SerialDescriptor,
        desc: ZeroJsonDescriptor
    ) {
        assertEquals(serialDesc, desc.serialDescriptor)
        assertEquals(serialDesc.elementsCount, desc.elementsCount)
        assertEquals(serialDesc.kind, desc.kind)
        assertEquals(serialDesc.kind.getFlags(), desc.kindFlags)
    }

    private fun ZeroJsonDescriptor.checkElementDescriptor(cache: DescriptorCache, index: Int) {
        val elementDescriptor = serialDescriptorUnsafe.getElementDescriptor(index)
        val descFromCache = cache.getOrCreateUnsafe(elementDescriptor)
        val actualDesc = getElementDescriptor(index, cache, elementDescriptor)
        assertSame(descFromCache, actualDesc, actualDesc.toString())
    }
}

@Suppress("TestFunctionName")
internal fun DescriptorCache(config: ZeroJsonConfiguration): DescriptorCache = DescriptorCache(config.descriptorCacheConfig)