package dev.dokky.zerojson.internal

import dev.dokky.zerojson.*
import dev.dokky.zerojson.ZeroJson.Default.configuration
import karamel.utils.MapEntry
import karamel.utils.assert
import kotlinx.serialization.json.JsonElement

@Suppress("OPT_IN_USAGE")
internal class JsonTreeDecodingStack(config: ZeroJsonConfiguration) {
    private var keys = arrayOfNulls<String>(config.maxStructureDepth * 16)
    private var values = arrayOfNulls<JsonElement>(config.maxStructureDepth * 16)
    private val dynamicElementCounts = IntArray(config.maxStructureDepth)
    private var dynamicElementsStart = 0
    private var totalElements = 0
    private var depth = 0

    fun clear() {
        keys.fill(null, toIndex = totalElements)
        values.fill(null, toIndex = totalElements)
        dynamicElementsStart = 0
        totalElements = 0
        depth = 0
    }

    private fun ensureCapacity() {
        if (totalElements <= keys.size) return

        val increment = ((keys.size * 1.6).toInt() - keys.size).coerceAtMost(20_000)
        keys = keys.copyOf(newSize = (keys.size + increment).coerceAtLeast(totalElements))
        values = values.copyOf(newSize = keys.size)
    }

    fun enter(descriptor: ZeroJsonDescriptor) {
        if (depth >= configuration.maxStructureDepth) {
            throw JsonMaxDepthReachedException()
        }

        totalElements += descriptor.totalElementCount
        dynamicElementsStart = totalElements

        ensureCapacity()

        dynamicElementCounts[depth] = 0
        depth++
    }

    fun leave(descriptor: ZeroJsonDescriptor) {
        assert { depth > 0 }

        val dynamicElementCount = dynamicElementCounts[--depth]
        dynamicElementCounts[depth] = 0

        val newSize = totalElements - descriptor.totalElementCount - dynamicElementCount
        keys.fill(null, fromIndex = newSize, toIndex = totalElements)
        values.fill(null, fromIndex = newSize, toIndex = totalElements)
        totalElements = newSize

        dynamicElementsStart = totalElements - currentMapSize
    }


    // static elements (inline properties)

    fun getElementKey(index: Int): String? = keys[arrayIndex(index)]

    fun getElementValue(index: Int): JsonElement? = values[arrayIndex(index)]

    fun putElement(index: Int, entry: Map.Entry<String, JsonElement>) {
        putElement(index, entry.key, entry.value)
    }

    fun putElement(index: Int, key: String, value: JsonElement) {
        val idx = arrayIndex(index)
        keys[idx] = key
        values[idx] = value
    }

    fun markElementIsPresent(index: Int) {
        val idx = arrayIndex(index)
        if (keys[idx] == null) keys[idx] = ""
    }

    fun tryMarkInlineSite(elementInfo: ElementInfo) {
        elementInfo.inlineSiteIndex.let { if (it >= 0) markElementIsPresent(it) }
    }

    fun isElementPresent(index: Int): Boolean = getElementKey(index) != null

    // masking the number in order to get the JIT recognize it as always positive
    private fun arrayIndex(index: Int): Int = (dynamicElementsStart - index - 1) and (0.inv() ushr 1)


    // dynamic elements (inline map entries)

    val currentMapSize: Int get() {
        val idx = depth - 1
        return if (idx < 0) 0 else dynamicElementCounts[idx]
    }

    fun addMapEntry(entry: Map.Entry<String, JsonElement>) {
        keys[totalElements] = entry.key
        values[totalElements] = entry.value
        dynamicElementCounts[depth - 1]++
        totalElements++
    }

    fun removeMapEntry(): Map.Entry<String, JsonElement>? {
        val deCountIndex = depth - 1
        val count = dynamicElementCounts[deCountIndex]
        if (count == 0) return null

        dynamicElementCounts[deCountIndex] = count - 1
        totalElements--
        val key = keys[totalElements]!!
        val value = values[totalElements]!!
        keys[totalElements] = null
        values[totalElements] = null
        return MapEntry(key, value)
    }
}