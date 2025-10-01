package dev.dokky.zerojson.internal

import dev.dokky.bitvector.MutableBitVector
import dev.dokky.bitvector.firstZero
import dev.dokky.zerojson.JsonMaxDepthReachedException
import io.kodec.buffers.ArrayDataBuffer
import io.kodec.struct.BufferStruct
import io.kodec.struct.BufferStructField
import io.kodec.struct.getBackwards
import io.kodec.struct.putBackwards
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlin.jvm.JvmStatic

internal class JsonTextDecodingStack(
    private val capacity: Int,
    maxInlinedElements: Int
) {
    val inlineRootStack = InlineRootStack(maxDepth = capacity, maxTotalElements = maxInlinedElements)

    private val descriptorStack = ArrayDeque<ZeroJsonDescriptor>(capacity)

    // cache current descriptor to eliminate range checks and
    // indirection of accessing last element in [descriptorStack]
    private var currentDescriptor: ZeroJsonDescriptor? = null

    private val flagsStack = MutableBitVector(capacity * 24)
    private var flagStackSize = 0

    private val elementMetadataStack = ArrayDataBuffer(capacity * MetadataFrameLayout.MAX_FRAME_SIZE, rangeChecks = false)
    private var elementMetadataStackSize = 0

    var maxDepth: Int = capacity
        private set

    val depth: Int get() = descriptorStack.size

    private fun elementFlagPos(elementIndex: Int): Int =
        flagStackSize - 1 - MetadataFrameLayout.FLAGS_COUNT - elementIndex

    private fun flagPos(flagId: Int): Int =
        flagStackSize - 1 - flagId

    private fun getFlag(flagId: Int): Boolean = flagsStack.unsafeGet(flagPos(flagId))
    private fun setFlag(flagId: Int, value: Boolean) = flagsStack.unsafeSet(flagPos(flagId), value)

    fun clear() {
        elementMetadataStack.clear(0, elementMetadataStackSize)
        flagsStack.clear(0 until flagStackSize)

        flagStackSize = 0
        elementMetadataStackSize = 0

        descriptorStack.clear()
        inlineRootStack.clear()

        currentDescriptor = null
    }

    fun prepare(maxDepth: Int) {
        if (maxDepth > capacity) error("maxDepth=$maxDepth > capacity=$capacity")
        this.maxDepth = maxDepth
    }

    fun enter(descriptor: ZeroJsonDescriptor, inlineElementsOffset: Int = 0) {
        enter(descriptor,
            inlineElementsOffset = inlineElementsOffset,
            polymorphicDecoding = false
        )
    }

    fun enterPolymorphic(
        descriptor: ZeroJsonDescriptor,
        discriminatorKeyStart: Int,
        discriminatorValueEnd: Int,
        shouldSkipDiscriminator: Boolean
    ) {
        enter(descriptor,
            inlineElementsOffset = 0,
            polymorphicDecoding = true
        )

        this.discriminatorKeyStart = discriminatorKeyStart
        this.discriminatorValueEnd = discriminatorValueEnd
        this.shouldSkipDiscriminator = shouldSkipDiscriminator
    }

    private fun enter(
        descriptor: ZeroJsonDescriptor,
        inlineElementsOffset: Int,
        polymorphicDecoding: Boolean
    ) {
        if (descriptorStack.size == maxDepth) throw JsonMaxDepthReachedException()

        descriptorStack.add(descriptor)
        currentDescriptor = descriptor

        val elementsCount = descriptor.elementsCount

        flagStackSize += elementsCount + MetadataFrameLayout.FLAGS_COUNT

        val layoutSize = when {
            polymorphicDecoding -> StackFrameFieldsForPolyObject.SIZE
            else -> StackFrameFieldsBase.SIZE
        }
        elementMetadataStackSize += layoutSize + 1 // 1 extra byte for size itself
        elementMetadataStack[elementMetadataStackSize - 1] = layoutSize

        this.inlineElementsOffset = inlineElementsOffset
        elementIndex = -1
    }

    fun leave(descriptor: SerialDescriptor): ZeroJsonDescriptor {
        val removed = descriptorStack.removeLast()
        debugAssert { removed.check(descriptor) }
        currentDescriptor = descriptorStack.lastOrNull()

        val layoutSize = elementMetadataStack[elementMetadataStackSize - 1]
        val newMetaPtr = elementMetadataStackSize - layoutSize - 1 // 1 extra byte for size itself
        val newFlagsPtr = flagStackSize - removed.elementsCount - MetadataFrameLayout.FLAGS_COUNT

        elementMetadataStack.clear(newMetaPtr, elementMetadataStackSize)
        flagsStack.clear(newFlagsPtr, flagStackSize)

        elementMetadataStackSize = newMetaPtr
        flagStackSize = newFlagsPtr
        return removed
    }

    fun descriptor(): ZeroJsonDescriptor = currentDescriptor!!

    fun serialDescriptor(): SerialDescriptor = descriptor().serialDescriptor

    fun nonInlineDescriptor(): ZeroJsonDescriptor =
        if (isInlined) inlineRootStack.descriptor() else descriptor()

    private fun getElementMetadata(frameStart: Int, field: BufferStructField<Int>): Int =
        elementMetadataStack.getBackwards(structOffset = frameStart, field = field)

    private fun getElementMetadata(field: BufferStructField<Int>): Int =
        getElementMetadata(frameStart = elementMetadataStackSize - 1, field)

    private fun setElementMetadata(field: BufferStructField<Int>, value: Int) {
        elementMetadataStack.putBackwards(structOffset = elementMetadataStackSize - 1, field = field, value = value)
    }

    var elementIndex: Int
        get() = getElementMetadata(StackFrameFieldsBase.elementIndex)
        set(value) = setElementMetadata(StackFrameFieldsBase.elementIndex, value)

    /** @return 0 if current descriptor is NOT inline element itself */
    var inlineElementsOffset: Int
        get() = getElementMetadata(StackFrameFieldsBase.inlineElementsOffset)
        private set(value) = setElementMetadata(StackFrameFieldsBase.inlineElementsOffset, value)

    var discriminatorKeyStart: Int
        get() = getElementMetadata(StackFrameFieldsForPolyObject.discriminatorStart)
        set(value) = setElementMetadata(StackFrameFieldsForPolyObject.discriminatorStart, value)

    var discriminatorValueEnd: Int
        get() = getElementMetadata(StackFrameFieldsForPolyObject.discriminatorEnd)
        set(value) = setElementMetadata(StackFrameFieldsForPolyObject.discriminatorEnd, value)

    var currentKeyStart: Int
        get() = getElementMetadata(StackFrameFieldsBase.keyStart)
        set(value) = setElementMetadata(StackFrameFieldsBase.keyStart, value)

    fun incAndGetCurrentElementIndex(): Int {
        val result = elementIndex + 1
        elementIndex = result
        return result
    }

    fun markObjectPropertyIsPresent(elementIndex: Int): Boolean {
        validateElementIndex(elementIndex)
        val wasSetBefore = flagsStack.unsafeGetAndSet(elementFlagPos(elementIndex))
        return !wasSetBefore
    }

    fun markNextMissingElementIndex(descriptor: ZeroJsonDescriptor): Int {
        return nextZeroFlagIndex(descriptor).let { idx ->
            if (idx == -1) CompositeDecoder.DECODE_DONE else {
                flagsStack.unsafeSet(idx)
                (flagStackSize - 1 - MetadataFrameLayout.FLAGS_COUNT - idx)
            }
        }
    }

    fun allElementsArePresent(descriptor: ZeroJsonDescriptor): Boolean =
        nextZeroFlagIndex(descriptor) < 0

    private fun nextZeroFlagIndex(descriptor: ZeroJsonDescriptor): Int = flagsStack.firstZero(
        start = flagStackSize - MetadataFrameLayout.FLAGS_COUNT - descriptor.elementsCount,
        endExclusive = flagStackSize - MetadataFrameLayout.FLAGS_COUNT
    )

    var isDecodingNull: Boolean
        set(value) = setFlag(MetadataFrameLayout.FLAG_ID_DECODING_NULL, value)
        get() = getFlag(MetadataFrameLayout.FLAG_ID_DECODING_NULL)

    var shouldSkipDiscriminator: Boolean
        set(value) = setFlag(MetadataFrameLayout.FLAG_ID_SKIP_DISCRIMINATOR, value)
        get() = getFlag(MetadataFrameLayout.FLAG_ID_SKIP_DISCRIMINATOR)

    fun unmarkDecodingMissingNullable(): Boolean {
        val flagPos = flagPos(MetadataFrameLayout.FLAG_ID_DECODING_NULL)
        val result = flagsStack.unsafeGet(flagPos)
        if (result) flagsStack.unsafeUnset(flagPos)
        return result
    }

    val isInlined: Boolean get() = (inlineElementsOffset != 0)

    val isInlineRoot: Boolean get() = depth == inlineRootStack.getDecoderStackDepth()

    val isInlinedOrInlineRoot: Boolean get() = isInlined || isInlineRoot

    private inline fun iterate(process: (index: Int, metadataFrameStart: Int, flagFrameStart: Int) -> Unit) {
        var metadataStart = elementMetadataStackSize - 1
        var flagsStart = flagStackSize - 1

        for (i in descriptorStack.indices.reversed()) {
            val descriptor = descriptorStack[i]

            process(i, metadataStart, flagsStart)

            val frameSize = elementMetadataStack[metadataStart]
            metadataStart -= frameSize + 1 // 1 byte is frame size itself

            flagsStart -= MetadataFrameLayout.FLAGS_COUNT + descriptor.elementsCount
        }
    }

    fun currentJsonPath(
        reader: JsonReaderImpl,
        stringBuilder: StringBuilder = StringBuilder()
    ): String {
        val framePositions = IntArray(descriptorStack.size)
        iterate { index, metadataFrameStart, _ ->
            framePositions[index] = metadataFrameStart
        }

        return karamel.utils.buildString(stringBuilder) {
            for (i in descriptorStack.indices) {
                val descriptor = descriptorStack[i]

                if (descriptor === ZeroJsonDescriptor.ROOT) {
                    append('$')
                    continue
                }

                val frameStart = framePositions[i]

                val elementIndex = getElementMetadata(frameStart, StackFrameFieldsBase.elementIndex)
                if (elementIndex < 0) break

                if (elementIndex < descriptor.elementsCount && descriptor.isElementJsonInline(elementIndex)) continue

                if (descriptor.kind == StructureKind.LIST) {
                    append('[')
                    append(elementIndex)
                    append(']')
                    continue
                }

                // check if path goes inside object key
                if (descriptor.kind == StructureKind.MAP && elementIndex and 1 == 0) break

                val keyStart = elementMetadataStack.getBackwards(frameStart, StackFrameFieldsBase.keyStart)
                if (keyStart <= 0) break

                if (tryAppendSegment(reader.input, keyStart)) break
            }
        }
    }



    private fun validateElementIndex(elementIndex: Int) {
        if (DebugMode) doValidateElementIndex(elementIndex)
    }

    private fun doValidateElementIndex(elementIndex: Int) {
        val descriptor = serialDescriptor()
        if (elementIndex !in 0 until descriptor.elementsCount)
            error("invalid element index of '${descriptor.serialName}': $elementIndex")
    }
}

private object MetadataFrameLayout {
    @JvmStatic
    val MAX_FRAME_SIZE: Int get() = arrayOf(
        StackFrameFieldsBase.SIZE,
        StackFrameFieldsForPolyObject.SIZE
    ).max()

    const val FLAG_ID_DECODING_NULL = 0
    const val FLAG_ID_SKIP_DISCRIMINATOR = 1

    const val FLAGS_COUNT = 2
}

private object StackFrameFieldsBase: BufferStruct() {
    @JvmStatic val elementIndex = int24()
    @JvmStatic val inlineElementsOffset = int16()
    @JvmStatic val keyStart = int24()

    @JvmStatic val SIZE = getStructSize()
}

@Suppress("unused")
private object StackFrameFieldsForPolyObject: BufferStruct() {
    @JvmStatic val elementIndex = field(StackFrameFieldsBase.elementIndex)
    @JvmStatic val inlineElementsOffset = field(StackFrameFieldsBase.inlineElementsOffset)
    @JvmStatic val keyStart = field(StackFrameFieldsBase.keyStart)

    @JvmStatic val discriminatorStart = int24()
    @JvmStatic val discriminatorEnd = int24()

    @JvmStatic val SIZE = getStructSize()
}