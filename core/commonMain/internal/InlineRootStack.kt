package dev.dokky.zerojson.internal

import dev.dokky.zerojson.internal.InlineRootStack.Companion.KEY_OFFSET_BITS
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.jvm.JvmStatic

internal class InlineRootStack(maxDepth: Int, maxTotalElements: Int) {
    private val descriptors = ArrayList<ZeroJsonDescriptor>(maxDepth)
    private val mapKeyPositions = FramedIntStack(initialCapacity = 100)

    /**
     * Here we are saving positions of "deferred" JSON object elements.
     * We're also keeping here some extra metadata like the position
     * of root object closing bracket and current stack depth.
     *
     * Each element of the array encoded as pair integers:
     * * key position ([KEY_OFFSET_BITS] bits)
     * * value offset relative to the key position (`32 - `[KEY_OFFSET_BITS] bits)
     */
    private val elementPositions = IntArray(maxTotalElements + maxDepth * SPECIAL_POSITIONS)
    private var elementPositionsSize = 0

    fun clear() {
        descriptors.clear()

        elementPositions.fill(0, fromIndex = 0, toIndex = elementPositionsSize)
        elementPositionsSize = 0

        mapKeyPositions.clear()
    }

    fun enter(descriptor: ZeroJsonDescriptor, decoderStackDepth: Int) {
        debugAssert { descriptor.hasJsonInlineElements }

        descriptors.add(descriptor)
        mapKeyPositions.enter()

        elementPositionsSize += descriptor.totalElementCount + SPECIAL_POSITIONS
        if (elementPositionsSize >= elementPositions.size) throw SerializationException(
            "too many inlined elements in current deserialization. Try increase 'maxInlineProperties'"
        )
        setSpecialMetadata(SP_STACK_DEPTH, decoderStackDepth)
    }

    fun leave(serialDescriptor: SerialDescriptor) {
        val removed = descriptors.removeLast()
        debugAssert { removed.check(serialDescriptor) }

        mapKeyPositions.leave()

        val newSize = elementPositionsSize - removed.totalElementCount - SPECIAL_POSITIONS
        elementPositions.fill(0, fromIndex = newSize, toIndex = elementPositionsSize)
        elementPositionsSize = newSize
    }

    /** Current inline root descriptor */
    fun descriptor(): ZeroJsonDescriptor = descriptors.last()

    /** Current inline root descriptor or `null` if stack is empty */
    fun descriptorOrNull(): ZeroJsonDescriptor? = descriptors.lastOrNull()

    /** @see [JsonTextDecoder.discriminatorKeyStart] */
    fun getDiscriminatorKeyStart(): Int = getSpecialMetadata(SP_DISCR_KEY_START)
    /** @see [JsonTextDecoder.discriminatorValueEnd] */
    fun getDiscriminatorValueEnd(): Int = getSpecialMetadata(SP_DISCR_VALUE_END)

    /**
     * @see [JsonTextDecoder.discriminatorKeyStart]
     * @see [JsonTextDecoder.discriminatorValueEnd]
     */
    fun setDiscriminatorInfo(keyStart: Int, valueEnd: Int) {
        setSpecialMetadata(SP_DISCR_KEY_START, keyStart)
        setSpecialMetadata(SP_DISCR_VALUE_END, valueEnd)
    }

    /** The position of inline root closing bracket */
    fun getInlineRootEndPosition(): Int = getSpecialMetadata(SP_PARENT_END)
    fun setInlineRootEndPosition(position: Int) = setSpecialMetadata(SP_PARENT_END, position)

    fun getDecoderStackDepth(): Int {
        val idx = elementPositionsSize - 1 - SP_STACK_DEPTH
        if (idx < 0) return -1
        return elementPositions[idx]
    }

    private fun arrayIndex(elementIndex: Int): Int = elementPositionsSize - elementIndex - 1 - SPECIAL_POSITIONS

    fun getElementPosition(elementIndex: Int): Int = elementPositions[arrayIndex(elementIndex)]

    /**
     * @param keyOffset inverted position of key relative to [valuePosition]. Should be positive.
     */
    fun setElementPosition(elementIndex: Int, valuePosition: Int, keyOffset: Int) {
        elementPositions[arrayIndex(elementIndex)] = encodeElementInfo(
            valuePosition = valuePosition,
            keyOffset = keyOffset
        )
    }

    fun markElementInline(elementIndex: Int) {
        elementPositions[arrayIndex(elementIndex)] = ELEMENT_IS_INLINE_POS
    }

    fun markParentElementIfInline(elementInfo: ElementInfo) {
        elementInfo.inlineSiteIndex.let { if (it >= 0) markElementInline(elementIndex = it) }
    }

    fun addMapKeyPosition(position: Int) {
        mapKeyPositions.add(position)
    }

    fun removeMapKeyPosition(): Int = mapKeyPositions.removeLast()

    val mapKeyPositionCount: Int get() = mapKeyPositions.size

    private fun getSpecialMetadata(offset: Int): Int = elementPositions[elementPositionsSize - 1 - offset]

    private fun setSpecialMetadata(offset: Int, value: Int) {
        elementPositions[elementPositionsSize - 1 - offset] = value
    }

    companion object {
        // number of special "positions" reserved at the end of each frame
        private const val SPECIAL_POSITIONS = 4

        // special "positions" offsets
        private const val SP_PARENT_END = 0
        private const val SP_STACK_DEPTH = 1
        private const val SP_DISCR_KEY_START = 2
        private const val SP_DISCR_VALUE_END = 3

        private const val KEY_OFFSET_BITS = 8

        private const val KEY_OFFSET_SHIFT = (32 - KEY_OFFSET_BITS)
        const val MAX_ELEMENT_KEY_OFFSET = 0.inv() ushr KEY_OFFSET_SHIFT
        const val MAX_ELEMENT_VALUE_POSITION = 0.inv() ushr KEY_OFFSET_BITS

        /** special marker meaning that the element is marked with @JsonInline */
        const val ELEMENT_IS_INLINE_POS = 1

        @JvmStatic
        fun encodeElementInfo(valuePosition: Int, keyOffset: Int): Int {
            val keyOffset = if (keyOffset > MAX_ELEMENT_KEY_OFFSET) 0 else keyOffset
            return valuePosition or (keyOffset shl KEY_OFFSET_SHIFT)
        }
        @JvmStatic
        fun elementValuePosition(int: Int): Int = int and ((0.inv()) ushr KEY_OFFSET_BITS)
        @JvmStatic
        fun elementKeyOffset(int: Int): Int = int ushr KEY_OFFSET_SHIFT
    }
}