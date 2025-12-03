package dev.dokky.zerojson.internal

import dev.dokky.pool.SimpleObjectPool
import dev.dokky.zerojson.*
import io.kodec.buffers.ArrayBuffer
import io.kodec.text.*
import karamel.utils.assert
import karamel.utils.unsafeCast
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * WARN! Do not forget to call [startReading] before each deserialization!
 */
internal class JsonTextDecoder(
    val context: JsonContext,
    override val reader: JsonReaderImpl,
    private val ktxJson: Json?
): ZeroJsonBaseDecoder,
    ZeroJsonTextDecoder,
    CompositeDecoder,
    CompositeKeyDecoder,
    AutoCloseable
{
    private val stack = context.textDecodingStack // store local for fast access

    private val tempJsonKeyBuffer = ArrayBuffer(config.maxKeyLengthBytes)
    private val tempSimpleSubString: SimpleSubString = "".substringWrapper()
    private val tempTrSubString = RandomAccessTextReaderSubString()

    val config: ZeroJsonConfiguration get() = context.config
    override val zeroJson: ZeroJson get() = context.zeroJson
    override val serializersModule: SerializersModule get() = config.serializersModule

    /**
     * The variable is only used during [JsonInline] decoding.
     * Non-zero value indicates an offset corresponding to the beginning of [SerialDescriptor] element indices
     * inside array of flattened elements in inline-root [ZeroJsonDescriptor].
     * The variable initialized in [decodeElementIndex] and resets to zero in [beginStructure].
     */
    private var prepareInlineDecodingOffset = 0

    /** When initialized, [context] will be released at last [endStructure] or [clear]. */
    private var contextPool: SimpleObjectPool<JsonContext>? = null
    /** Decoder that allocated this decoder for compound key decoding */
    private var parentDecoder: CompositeKeyDecoder? = null
    /** Allocated child decoder for compound key */
    private var compoundChildDecoder: JsonTextDecoder? = null

    /**
     * Initialized in [preparePolymorphicDecoding].
     * If next call:
     * * is [beginStructure] -> this data used to initialize polymorphic stack frame, then cleared.
     * * any decoding primitive function -> decoder treats it as an
     * indicator of polymorphic value subclass decoding.
     */
    private var discriminatorKeyStart = 0
    /**
     * Position of first non-whitespace character after discriminator value
     * Expected to be comma or closing curly bracket
     */
    private var discriminatorValueEnd = 0
    private var lookupResult: PolymorphicSerializerCache.DeserializerLookupResult? = null
    /**
     * Position of first non-discriminator key in polymorphic JSON object.
     * Initialized in [preparePolymorphicDecoding].
     * Used only in [preparePolymorphicDecoding] and [decodeSerializableValue].
     */
    private var firstNonDiscKeyStart = 0

    /**
     * Used in [decodeJsonObject] and [decodeJsonArray] for controlling maximum JSON structure depth.
     */
    private var jsonElementDepth = 0

    fun startReading() {
        reader.skipWhitespace()
        stack.prepare(config.maxStructureDepth)
    }

    override fun close() {
        jsonElementDepth = 0
        prepareInlineDecodingOffset = 0
        firstNonDiscKeyStart = 0
        clearDiscriminatorState()
        tempTrSubString.clear()
        tempSimpleSubString.clear()
        compoundChildDecoder?.let {
            it.close()
            compoundChildDecoder = null
        }
        contextPool?.let { pool ->
            context.endRead()
            pool.release(context)
            contextPool = null
        }
    }

    override fun compositeChildDecoderReleased() {
        compoundChildDecoder = null
    }

    private fun clearDiscriminatorState() {
        discriminatorKeyStart = 0
        discriminatorValueEnd = 0
        lookupResult = null
    }

    override val json: Json
        get() = ktxJson ?: throwExpectedKotlinxEndec(decoder = true)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        var zDescriptor: ZeroJsonDescriptor?
        val discriminatorKeyStart = discriminatorKeyStart
        val discriminatorValueEnd = discriminatorValueEnd

        if (discriminatorKeyStart != 0) {
            zDescriptor = lookupResult?.descriptor
            when {
                zDescriptor == null -> zDescriptor = getZDescriptor(descriptor, context)
                zDescriptor.kind == StructureKind.MAP && descriptor.isMapWithStructuredKey(config) ->
                    zDescriptor = ZeroJsonDescriptor.LIST
            }
            checkEqualSerialNames(zDescriptor.serialName, descriptor.serialName)
            assert { prepareInlineDecodingOffset == 0 }
            stack.enterPolymorphic(
                zDescriptor,
                discriminatorKeyStart = discriminatorKeyStart,
                discriminatorValueEnd = discriminatorValueEnd,
                shouldSkipDiscriminator = !lookupResult.isDiscriminatorPresent()
            )
            clearDiscriminatorState()
        } else {
            val elementIndex = stack.elementIndex
            val parentDescriptor = stack.descriptor()
            if (config.structuredMapKeysMode != StructuredMapKeysMode.LIST &&
                parentDescriptor.kindFlags.isMapKey(elementIndex)) {
                return compoundKeyDecoder(descriptor).beginStructure(descriptor)
            }
            zDescriptor = parentDescriptor.getElementDescriptor(elementIndex, descriptor)
            if (zDescriptor.kind == StructureKind.MAP && descriptor.isMapWithStructuredKey(config)) {
                if (prepareInlineDecodingOffset > 0) throwInlineMapCompositeKeysAreNotSupported()
                zDescriptor = ZeroJsonDescriptor.LIST
            }
            stack.enter(zDescriptor, inlineElementsOffset = prepareInlineDecodingOffset)
        }

        // check if current descriptor is inline root
        if (prepareInlineDecodingOffset <= 0 &&
            zDescriptor.hasJsonInlineElements &&
            stack.inlineRootStack.descriptorOrNull() !== zDescriptor)
        {
            stack.inlineRootStack.enter(zDescriptor, stack.depth)
            if (!lookupResult.isDiscriminatorPresent()) stack.inlineRootStack.setDiscriminatorInfo(
                keyStart = discriminatorKeyStart,
                valueEnd = discriminatorValueEnd
            )
        }
        prepareInlineDecodingOffset = 0

        if (!stack.isInlined) reader.expectToken(zDescriptor.kind.openingBracket())
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (stack.depth == 0) unpairedEndStructure(descriptor)

        if (!stack.isInlined) {
            if (stack.isInlineRoot) {
                val inlineRootEnd = stack.inlineRootStack.getInlineRootEndPosition()
                // For inline-map inlineRootEnd is not initialized
                if (inlineRootEnd > 0) reader.position = inlineRootEnd
                stack.inlineRootStack.leave(descriptor)
            }

            val closingBracket = stack.descriptor().kind.closingBracket()
            if (!reader.trySkipToken(closingBracket)) {
                reader.skipObjectOrArray(openingBracket = closingBracket.toOpeningBracket())
            }
        }

        stack.leave(descriptor)

        if (stack.depth == 1) {
            val parent = parentDecoder
            if (parent != null) expectEndOfStructuredKey()
            close() // must come before compositeChildDecoderReleased() to prevent any memory leaks
            parent?.compositeChildDecoderReleased()
        }
    }

    private fun expectEndOfStructuredKey() {
        val nextCp = reader.nextCodePoint
        if (nextCp >= 0) reader.unexpectedChar(nextCp)
    }

    private fun unpairedEndStructure(descriptor: SerialDescriptor): Nothing =
        error("attempt to invoke endStructure() without preceding beginStructure() with descriptor of type '${descriptor.serialName}'")

    private fun trySkipCollectionComma(
        prevIdx: Int,
        returnIdx: Int,
        closingBracket: Char
    ): Int = when {
        prevIdx >= 0 && !reader.trySkipComma(closingBracket) -> CompositeDecoder.DECODE_DONE
        reader.nextIs(closingBracket) -> CompositeDecoder.DECODE_DONE
        else -> returnIdx
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val zDescriptor = stack.descriptor()

        if (stack.isInlinedOrInlineRoot &&
            stack.inlineRootStack.getInlineRootEndPosition() > 0) {
            return decodeScannedElementIndex(descriptor, zDescriptor)
        }

        return when (zDescriptor.kind) {
            StructureKind.LIST -> decodeListElementIndex(descriptor)
            StructureKind.MAP -> decodeMapElementIndex(descriptor)
            else -> decodePropertyIndex(stack.descriptor()).also {
                stack.elementIndex = it
            }
        }
    }

    private fun decodeScannedElementIndex(serialDesc: SerialDescriptor, descriptor: ZeroJsonDescriptor): Int {
        return if (descriptor.kind == StructureKind.MAP) {
            decodeScannedMapIndex(serialDesc,
                index = stack.incAndGetCurrentElementIndex())
        } else {
            decodeScannedPropertyIndex(descriptor).also { idx ->
                stack.elementIndex = idx
            }
        }
    }

    private fun decodeMapElementIndex(serialDesc: SerialDescriptor): Int {
        val prevIdx = stack.elementIndex
        val idx = prevIdx + 1
        stack.elementIndex = idx
        return when {
            // index is odd -> reading value
            idx and 1 == 1 -> { reader.expectColon(); idx }
            stack.isInlined -> decodeInlinedMapKeyIndex(serialDesc, idx)
            else -> {
                stack.currentKeyStart = 0
                trySkipCollectionComma(prevIdx = prevIdx, returnIdx = idx, closingBracket = '}').also { idx ->
                    if (idx >= 0) stack.currentKeyStart = reader.position
                }
            }
        }
    }

    private fun decodeListElementIndex(descriptor: SerialDescriptor): Int {
        val prevIdx = stack.elementIndex
        val idx = prevIdx + 1
        stack.elementIndex = -1
        val result = trySkipCollectionComma(prevIdx = prevIdx, returnIdx = idx, closingBracket = ']')
        if (result == CompositeDecoder.DECODE_DONE && descriptor.kind != StructureKind.LIST) {
            checkIfMapIsMissingValue(prevIdx)
        }
        stack.elementIndex = idx
        return result
    }

    private fun decodeInlinedMapKeyIndex(serialDesc: SerialDescriptor, idx: Int): Int {
        assert { idx and 1 == 0 }

        val discriminatorKeyStart = stack.inlineRootStack.getDiscriminatorKeyStart()
        var expectComma = idx
        var endOfObject: Boolean

        do {
            endOfObject = if (expectComma != 0) !reader.trySkipObjectComma() else reader.nextIs('}')
            if (endOfObject) break

            val keyStart = reader.position
            if (keyStart == discriminatorKeyStart) {
                reader.position = stack.inlineRootStack.getDiscriminatorValueEnd()
                expectComma = 1
                continue
            }

            val key = reader.readJsonSubString()
            reader.expectColon()

            val inlineRootDescriptor = stack.inlineRootStack.descriptor()
            val elementInfo = inlineRootDescriptor.getElementInfo(key)
            if (elementInfo == inlineRootDescriptor.inlineMapElement) {
                // element belong to the inlined Map we're currently decoding
                stack.currentKeyStart = keyStart
                if (config.coerceInputValues && trySkipMapValue(serialDesc, skipKvSeparator = false)) {
                    expectComma = 0 // trySkipMapValue skips comma
                    continue
                }
                reader.position = keyStart
                break
            }

            // Element is not belong to the inlined Map.
            // Save its position for future decoding.
            stack.inlineRootStack.setElementPosition(
                elementIndex = elementInfo.index,
                valuePosition = reader.position,
                keyOffset = reader.position - keyStart
            )
            stack.inlineRootStack.markParentElementIfInline(elementInfo)
            endOfObject = !reader.skipToNextKey()
            expectComma = 0
        } while (!endOfObject)

        return if (!endOfObject) idx else {
            stack.inlineRootStack.setInlineRootEndPosition(reader.position)
            decodeScannedMapIndex(serialDesc, idx)
        }
    }

    private fun decodeScannedMapIndex(serialDesc: SerialDescriptor, index: Int): Int {
        debugAssert { serialDesc.kind == StructureKind.MAP }

        if (index and 1 == 1) {
            // Element index is odd -> reading value.
            reader.expectColon()
        } else {
            while (true) {
                if (stack.inlineRootStack.mapKeyPositionCount == 0) {
                    stack.currentKeyStart = 0
                    return CompositeDecoder.DECODE_DONE
                }
                val keyStart = stack.inlineRootStack.removeMapKeyPosition()
                reader.position = keyStart
                stack.currentKeyStart = keyStart
                if (!config.coerceInputValues || !trySkipMapValue(serialDesc, skipKvSeparator = true)) return index
            }
        }

        return index
    }

    private fun decodeScannedPropertyIndex(descriptor: ZeroJsonDescriptor): Int {
        while (true) {
            val idx = stack.markNextMissingElementIndex(descriptor)
            if (idx < 0) {
                stack.currentKeyStart = 0
                return CompositeDecoder.DECODE_DONE
            }

            val rootDesc = stack.inlineRootStack.descriptor()
            val elementsOffset = stack.inlineElementsOffset
            val absoluteIdx = elementsOffset + idx

            // searching "deferred" element by position from decoderStack.inlineRootStack
            val elementPos = stack.inlineRootStack.getElementPosition(absoluteIdx)
            if (elementPos != 0) { // the element is present in JSON
                if (elementPos == InlineRootStack.ELEMENT_IS_INLINE_POS) {
                    prepareInlineDecodingOffset = rootDesc.getChildElementsOffset(absoluteIdx)
                } else { // the element is NOT marked with @JsonInline
                    reader.position = InlineRootStack.elementValuePosition(elementPos)
                    stack.currentKeyStart = reader.position - InlineRootStack.elementKeyOffset(elementPos)
                    if (config.coerceInputValues && trySkipOrCoerceProperty(stack.descriptor(), idx)) continue
                }
                return idx
            } else {
                val childElementsOffset = rootDesc.getChildElementsOffset(absoluteIdx)

                if (childElementsOffset != 0) { // the element is inlined
                    // Sometimes, @JsonInline element does not have any element present except
                    // some nested @JsonInline element. In such case inlineRootStack.getElementPosition()
                    // will return 0 because inlineRootStack.markElementInline only marks closest parent
                    // but not parent of parent etc.
                    // Here we're checking if current element is @JsonInline (absoluteIdx != 0),
                    // then we iterate inline subtree and search deeply nested elements.
                    if (anyElementExistsInInlineSubTree(
                        rootDesc = rootDesc,
                        serialDesc = descriptor.serialDescriptorUnsafe,
                        elementIndex = idx,
                        childElementsOffset = childElementsOffset))
                    {
                        prepareInlineDecodingOffset = childElementsOffset
                        return idx
                    }
                }

                // the element is NOT present in JSON

                val isOptional = descriptor.serialDescriptorUnsafe.isElementOptional(idx)
                val canBeImplicitNull = !isOptional && !config.explicitNulls &&
                    descriptor.serialDescriptorUnsafe.getElementDescriptor(idx).isDeepNullable()

                if (childElementsOffset != 0 && !canBeImplicitNull && !isOptional) {
                    // The element is inlined and it is not optional.
                    // So either it's can be empty or not present at all - we should enter.
                    prepareInlineDecodingOffset = childElementsOffset
                    return idx
                }

                if (canBeImplicitNull) {
                    stack.isDecodingNull = true
                    return idx
                }
            }
        }
    }

    private fun anyElementExistsInInlineSubTree(
        rootDesc: ZeroJsonDescriptor,
        serialDesc: SerialDescriptor,
        elementIndex: Int,
        childElementsOffset: Int
    ): Boolean = anyElementExistsInInlineSubTreeTemplate(
        rootDesc = rootDesc,
        serialDesc = serialDesc,
        elementIndex = elementIndex,
        childElementsOffset = childElementsOffset,
        isElementPresent = { absIdx -> stack.inlineRootStack.getElementPosition(absIdx) != 0 },
        markElementPresent = { absIdx -> stack.inlineRootStack.markElementInline(absIdx) },
        callRecursive = { rootDesc, serialDesc, elementIndex, childElementsOffset ->
            anyElementExistsInInlineSubTree(rootDesc, serialDesc, elementIndex, childElementsOffset)
        }
    )

    private fun decodePropertyIndex(descriptor: ZeroJsonDescriptor): Int {
        val positionBeforeKey = reader.position

        val prevIdx = stack.elementIndex
        stack.elementIndex = -1
        if (prevIdx >= 0) reader.trySkipObjectComma()

        keyLoop@ while (true) {
            if (reader.nextIs('}')) {
                return if (stack.isInlinedOrInlineRoot) {
                    // key positions are not preserved in scanned elements
                    stack.currentKeyStart = 0
                    stack.inlineRootStack.setInlineRootEndPosition(reader.position)
                    decodeScannedPropertyIndex(descriptor)
                } else {
                    getNextMissingNullablePropertyIndex(descriptor)
                }
            }

            val keyStart = reader.position

            if (stack.shouldSkipDiscriminator && keyStart == stack.discriminatorKeyStart) {
                reader.position = stack.discriminatorValueEnd
                reader.trySkipObjectComma()
                continue@keyLoop
            }

            val lastNonInlineDesc = stack.nonInlineDescriptor()
            val key = reader.readJsonSubString()
            reader.expectColon()
            stack.currentKeyStart = keyStart
            // elementIndex will be initialized properly decodePropertyIndex finishes successfully
            // otherwise this will be an indicator to path decoder that current frame has read the key
            stack.elementIndex = 0
            val elementInfo = lastNonInlineDesc.getElementInfo(key)

            if (elementInfo.isUnknown) {
                stack.elementIndex = 0
                if (!descriptor.ignoreUnknownKeys) unknownKeyError(key, position = key.start)
                reader.skipToNextKey()
                continue@keyLoop
            }

            val elementsOffset = stack.inlineElementsOffset
            val elementsEndOffset = elementsOffset + descriptor.elementsCount

            if (elementInfo.index in elementsOffset ..< elementsEndOffset) {
                // element belong to the current descriptor
                val elementIndex = elementInfo.index - elementsOffset
                if (!stack.markObjectPropertyIsPresent(elementIndex)) {
                    duplicateFieldError(elementIndex, position = keyStart)
                }
                if (config.coerceInputValues && trySkipOrCoerceProperty(descriptor, elementIndex)) continue@keyLoop
                return elementIndex
            }

            stack.inlineRootStack.markParentElementIfInline(elementInfo)

            if (elementInfo.inlineSiteIndex in elementsOffset ..< elementsEndOffset) {
                // Element is inlined direct child of current descriptor.
                // Switching to decoding inlined element instead of delaying.
                // This can be considered as fast path when every inline element ordered depth-first.
                return decodeFirstDirectInlinedChildElement(elementInfo, elementsOffset, keyStart, lastNonInlineDesc)
            } else {
                // Element is not belong to the current descriptor nor to inlined direct child.
                if (stack.allElementsArePresent(descriptor)) {
                    // All elements of the current inlined class are decoded -> leave it.
                    // Decoder will go into a state of reading next key in parent class
                    // and will expect the standard beginning of the key, which can be preceded by a comma,
                    // Therefore, we return not to the beginning of the key, but to the position
                    // of a token before the key (comma or opening bracket).
                    reader.position = positionBeforeKey
                    return CompositeDecoder.DECODE_DONE
                }

                savePropertyPositionAndSkipToNext(elementInfo, keyStart, lastNonInlineDesc)
            }
        }
    }

    private fun decodeFirstDirectInlinedChildElement(
        elementInfo: ElementInfo,
        elementsOffset: Int,
        keyStart: Int,
        lastNonInlineDesc: ZeroJsonDescriptor
    ): Int {
        val inlineSiteIndex = elementInfo.inlineSiteIndex
        val inlineSiteElementIndex = inlineSiteIndex - elementsOffset
        if (!stack.markObjectPropertyIsPresent(inlineSiteElementIndex)) {
            duplicateFieldError(inlineSiteElementIndex, position = keyStart)
        }
        prepareInlineDecodingOffset = lastNonInlineDesc.getChildElementsOffset(inlineSiteIndex)
        reader.position = keyStart
        return inlineSiteElementIndex
    }

    private fun savePropertyPositionAndSkipToNext(
        elementInfo: ElementInfo,
        keyStart: Int,
        inlineRootDescriptor: ZeroJsonDescriptor
    ) {
        if (stack.inlineRootStack.getDiscriminatorKeyStart() == keyStart) {
            reader.position = stack.inlineRootStack.getDiscriminatorValueEnd()
            reader.trySkipObjectComma()
            return
        }

        // We can only save its position to process it later.
        if (inlineRootDescriptor.inlineMapElement == elementInfo) {
            stack.inlineRootStack.addMapKeyPosition(keyStart)
        } else {
            val valueStart = reader.position
            stack.inlineRootStack.setElementPosition(
                elementInfo.index,
                valuePosition = valueStart,
                keyOffset = valueStart - keyStart
            )
        }

        reader.skipToNextKey()
    }

    private fun trySkipOrCoerceProperty(descriptor: ZeroJsonDescriptor, elementIndex: Int): Boolean {
        val serialDescriptor = descriptor.serialDescriptorUnsafe
        val elementDescriptor = serialDescriptor.getElementDescriptor(elementIndex)
        if (serialDescriptor.isElementOptional(elementIndex)) {
            return trySkipOptionalElement(elementDescriptor)
        }
        if (!config.explicitNulls) {
            elementDescriptor.asNullableEnum()?.let { tryNullifyInvalidEnumValue(it) }
        }
        return false
    }

    private fun trySkipMapValue(
        mapDescriptor: SerialDescriptor,
        skipKvSeparator: Boolean
    ): Boolean {
        debugAssert { mapDescriptor.kind == StructureKind.MAP }

        val valueDescriptor = mapDescriptor.getElementDescriptor(1)
        if (valueDescriptor.unnestInline().kind == SerialKind.ENUM) {
            val oldPos = reader.position
            if (skipKvSeparator) {
                reader.skipString()
                reader.expectColon()
            }
            if (trySkipOptionalElement(valueDescriptor)) return true else {
                reader.position = oldPos
            }
        }

        return false
    }

    private fun tryNullifyInvalidEnumValue(enumDescriptor: SerialDescriptor) {
        if (reader.nextIs('n')) return
        val position = reader.position
        if (getElementInfo(enumDescriptor, reader.readJsonSubString()).isUnknown) {
            stack.isDecodingNull = true
        } else {
            reader.position = position
        }
    }

    private fun trySkipOptionalElement(elementDescriptor: SerialDescriptor): Boolean {
        var skipped = false
        val position = reader.position

        // fixme value class nesting
        if (reader.input.trySkipJsonNull()) {
            if (!elementDescriptor.isNullable) {
                reader.skipWhitespace()
                skipped = true
            }
        } else if (elementDescriptor.kind == SerialKind.ENUM) {
            if (getElementInfo(elementDescriptor, reader.readJsonSubString()).isUnknown) skipped = true
        }

        if (skipped) {
            reader.trySkipObjectComma()
            return true
        }

        reader.position = position
        return false
    }

    private fun duplicateFieldError(elementIndex: Int, position: Int): Nothing =
        duplicateFieldError(key = stack.descriptor().getElementName(elementIndex), position = position)

    private fun duplicateFieldError(key: Any, position: Int): Nothing =
        throw ZeroJsonDecodingException(
            message = "key '$key' encountered multiple times",
            position = position
        )

    private fun getNextMissingNullablePropertyIndex(descriptor: ZeroJsonDescriptor): Int {
        debugAssert { !stack.isInlinedOrInlineRoot }

        if (config.explicitNulls) return CompositeDecoder.DECODE_DONE

        val serialDescriptor = descriptor.serialDescriptor
        while (true) {
            val idx = stack.markNextMissingElementIndex(descriptor)
            if (idx < 0) return CompositeDecoder.DECODE_DONE

            if (!serialDescriptor.isElementOptional(idx) &&
                serialDescriptor.getElementDescriptor(idx).isDeepNullable())
            {
                stack.isDecodingNull = true
                return idx
            }
        }
    }

    @JvmOverloads
    internal fun JsonReaderImpl.readJsonSubString(
        requireQuotes: Boolean = config.expectStringQuotes,
        allowNull: Boolean = false,
    ): AbstractMutableSubString =
        when (val input = input) {
            is Utf8TextReader ->
                readString(input, tempTrSubString, tempSimpleSubString, requireQuotes = requireQuotes, allowNull = allowNull)
            is StringTextReader -> {
                readString(input, tempSimpleSubString, requireQuotes = requireQuotes, allowNull = allowNull)
                tempSimpleSubString
            }
        }

    private fun ZeroJsonDescriptor.getElementInfo(substring: AbstractMutableSubString): ElementInfo =
        getElementInfoByName(substring, tempJsonKeyBuffer)

    private val unsignedDecoder = JsonTextDecoderForUnsignedTypes(this)

    override fun decodeInline(descriptor: SerialDescriptor): ZeroJsonDecoder = when {
        descriptor.isUnsignedNumber -> unsignedDecoder
        else -> this
    }

    override fun decodeBoolean(): Boolean = reader.maybeQuoted { readBoolean(skipWhitespace = false) }

    override fun decodeByte(): Byte = reader.maybeQuoted { readByte(skipWhitespace = false) }

    override fun decodeShort(): Short = reader.maybeQuoted { readShort(skipWhitespace = false) }

    override fun decodeInt(): Int = reader.maybeQuoted { readInt(skipWhitespace = false) }

    override fun decodeLong(): Long = reader.maybeQuoted { readLong(skipWhitespace = false) }

    override fun decodeChar(): Char =
        reader.readString().let { s ->
            if (s.length != 1) throw ZeroJsonDecodingException("expected string with single character")
            s[0]
        }

    override fun decodeDouble(): Double = reader.maybeQuoted {
        readDouble(allowSpecial = config.allowSpecialFloatingPointValues, skipWhitespace = false)
    }

    override fun decodeFloat(): Float = reader.maybeQuoted {
        readFloat(allowSpecial = config.allowSpecialFloatingPointValues, skipWhitespace = false)
    }

    override fun decodeString(): String = reader.readString()

    override fun decodeStringChunked(consumeChunk: (chunk: String) -> Unit) {
        reader.readStringChunked(context.dataBuilder, acceptChunk = consumeChunk)
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val entryName = reader.readJsonSubString()
        val elementInfo = getElementInfo(enumDescriptor, entryName)
        if (elementInfo.isUnknown) unknownEnumEntry(entryName, position = entryName.start)
        return elementInfo.index
    }

    private fun getElementInfo(descriptor: SerialDescriptor, name: AbstractMutableSubString): ElementInfo =
        context.descriptorCache
            .getOrCreateUnsafe(descriptor)
            .getElementInfo(name)

    override fun decodeNotNullMark(): Boolean {
        if (stack.isDecodingNull) return false

        val start = reader.position
        val isNull = reader.input.trySkipJsonNull()
        reader.position = start
        return !isNull
    }

    override fun decodeNull(): Nothing? {
        if (stack.unmarkDecodingMissingNullable()) return null
        reader.readNull()
        return null
    }

    private fun compoundKeyDecoder(keyDescriptor: SerialDescriptor): JsonTextDecoder {
        config.throwIfStructuredKeysDisabled(keyDescriptor)
        if (compoundChildDecoder != null) {
            error("attempt to decoder composite key, but previous composite decoding is not finished with endStructure()")
        }
        val builder = context.dataBuilder
        builder.clear()
        reader.readString(output = builder.builder)
        builder.updateCapacity()
        return compoundKeyDecoder(zeroJson, parent = this, source = builder.builder).also {
            compoundChildDecoder = it
        }
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T = decodeSerializableValue(deserializer)

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        var actualDeserializer: DeserializationStrategy<T> = deserializer
        var outerPolyObjectEnd = 0

        if (deserializer is AbstractPolymorphicSerializer<*>) {
            val parentDescriptor = stack.descriptor()
            val elementIndex = stack.elementIndex

            clearDiscriminatorState()

            if (config.structuredMapKeysMode != StructuredMapKeysMode.LIST &&
                parentDescriptor.kindFlags.isMapKey(elementIndex))
                return compoundKeyDecoder(deserializer.descriptor)
                    .decodeSerializableValue(deserializer)

            if (firstNonDiscKeyStart != 0) {
                // actual deserializer is polymorphic itself
                outerPolyObjectEnd = beforeValueSubClass()
            }

            actualDeserializer = preparePolymorphicDecoding(
                deserializer,
                baseDescriptor = parentDescriptor.getElementDescriptor(elementIndex, deserializer.descriptor)
            ).unsafeCast()
        }

        val wrapped = firstNonDiscKeyStart != 0 && if (!lookupResult!!.descriptor.needWrappingIfSubclass()) {
            firstNonDiscKeyStart = 0
            false
        } else {
            true
        }

        val objectEnd = if (!wrapped) 0 else beforeValueSubClass()
        val result = actualDeserializer.deserialize(this)
        if (wrapped) afterValueSubClass(objectEnd)
        if (outerPolyObjectEnd != 0) afterValueSubClass(outerPolyObjectEnd)

        return result
    }

    private fun ZeroJsonDescriptor.getElementDescriptor(elementIndex: Int, elementDescriptor: SerialDescriptor): ZeroJsonDescriptor =
        getElementDescriptor(elementIndex, context.descriptorCache, elementDescriptor)

    private fun preparePolymorphicDecoding(
        deserializer: AbstractPolymorphicSerializer<*>,
        baseDescriptor: ZeroJsonDescriptor
    ): DeserializationStrategy<*> {
        debugAssert {
            !stack.isInlinedOrInlineRoot ||
            stack.inlineRootStack.getInlineRootEndPosition() == 0
        }
        checkEqualSerialNames(baseDescriptor.serialName, deserializer.descriptor.serialName)
        val objectStart = reader.position
        firstNonDiscKeyStart = 0
        reader.expectBeginObject()

        var lookupResult: PolymorphicSerializerCache.DeserializerLookupResult? = null
        if (!reader.nextIs('}')) {
            val discriminator = baseDescriptor.discriminatorSubStringFor(reader.input)!!
            do {
                val keyStart = reader.position
                val key = reader.readJsonSubString()
                reader.expectColon()
                if (key == discriminator) {
                    val discriminatorValue = reader.readNullable { reader.readJsonSubString(allowNull = true) }
                    if (discriminatorValue != null) {
                        val discriminatorValueEnd = reader.position
                        lookupResult = context.polymorphicDeserializerResolver
                            .lookup(this, deserializer, baseDescriptor, discriminatorValue)
                            ?: throwUnknownSubTypeError(deserializer, discriminatorValue)
                        this.discriminatorKeyStart = keyStart
                        this.discriminatorValueEnd = discriminatorValueEnd
                    }
                } else {
                    if (firstNonDiscKeyStart == 0) firstNonDiscKeyStart = keyStart
                    reader.skipElement(unchecked = true)
                }
                if (!reader.trySkipObjectComma()) break
            } while (discriminatorKeyStart == 0 || firstNonDiscKeyStart == 0)
        }
        if (lookupResult == null) {
            lookupResult = context.polymorphicDeserializerResolver
                .lookupDefaultDeserializer(deserializer, baseDescriptor)
                ?: missingDiscriminator(deserializer)
        }

        if (firstNonDiscKeyStart == 0) {
            // first key was discriminator, we stopped before next key or end of the object
            firstNonDiscKeyStart = reader.position
        }

        reader.position = objectStart
        this.lookupResult = lookupResult
        return lookupResult.deserializer
    }

    private fun beforeValueSubClass(): Int {
        reader.position = firstNonDiscKeyStart
        firstNonDiscKeyStart = 0

        var valueKeyStart = 0
        var valueStart = 0

        if (!reader.nextIs('}')) do {
            var keyStart = reader.position

            if (keyStart == discriminatorKeyStart) {
                reader.position = discriminatorValueEnd
                if (!reader.trySkipObjectComma()) break
                keyStart = reader.position
            }

            val key = reader.readJsonSubString()
            reader.expectColon()

            if (key == VALUE_SUBSTRING) {
                if (valueKeyStart != 0) duplicateFieldError(VALUE_SUBSTRING, keyStart)
                valueStart = reader.position
                valueKeyStart = keyStart
            } else if (!config.ignoreUnknownKeys) {
                unknownKeyError(key, position = keyStart)
            }
        } while (reader.skipToNextKey())

        val objectEnd = reader.position + 1 // skip closing brace

        var decodingNull = false
        if (valueStart == 0) {
            if (lookupResult!!.descriptor.isNullable && !config.explicitNulls) decodingNull = true
            else throw MissingFieldException(
                missingFields = listOf(VALUE_SUBSTRING.toString()),
                message = "Field 'value' is required for polymorphic subclass deserialization, but it was missing",
                cause = null
            )
        }

        stack.enter(ZeroJsonDescriptor.POLYMORPHIC_VALUE_WRAPPER)
        stack.elementIndex = 1
        if (decodingNull) {
            stack.isDecodingNull = true
        } else {
            stack.currentKeyStart = valueKeyStart
            reader.position = valueStart
        }
        return objectEnd
    }

    private fun afterValueSubClass(objectEndPosition: Int) {
        // Check if next element is comma or closing bracket.
        // This is necessary because beforeValueSubClass()
        // does not check actual value format.
        reader.trySkipObjectComma()

        assert { objectEndPosition != 0 }
        stack.leave(ZeroJsonDescriptor.POLYMORPHIC_VALUE_WRAPPER.serialDescriptorUnsafe)
        reader.position = objectEndPosition
        reader.skipWhitespace()
        clearDiscriminatorState()
    }

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): ZeroJsonDecoder =
        decodeInline(descriptor.getElementDescriptor(index))

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        // isNullable=true means deserializer can handle nullability by itself
        return if (deserializer.descriptor.isNullable || decodeNotNullMark()) {
            decodeSerializableElement(descriptor, index, deserializer)
        } else {
            decodeNull()
        }
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decodeBoolean()
    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decodeByte()
    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decodeShort()
    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decodeInt()
    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decodeLong()
    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decodeFloat()
    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decodeDouble()
    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decodeChar()
    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = decodeString()

    override fun decodeJsonElement(): JsonElement = when(reader.nextCodePoint) {
        '['.code -> decodeJsonArray()
        '{'.code -> decodeJsonObject()
        else -> decodeJsonPrimitive()
    }

    private fun <R: JsonElement> decodeCompositeJsonElement(block: () -> R): R {
        jsonElementDepth++
        if (jsonElementDepth > config.maxStructureDepth) throw JsonMaxDepthReachedException()
        return block().also { jsonElementDepth-- }
    }

    override fun decodeJsonObject(): JsonObject = decodeCompositeJsonElement {
        buildJsonObject {
            reader.readObject {
                while (hasMoreKeys()) {
                    if (reader.position == discriminatorKeyStart) {
                        if (!lookupResult!!.discriminatorPresent) {
                            reader.position = discriminatorValueEnd
                        }
                        clearDiscriminatorState()
                    } else {
                        val key = readKey()
                        val value = decodeJsonElement()
                        this@buildJsonObject.put(key, value)
                    }
                    reader.trySkipObjectComma()
                }
            }
        }
    }

    override fun decodeJsonArray(): JsonArray = decodeCompositeJsonElement {
        buildJsonArray {
            reader.readArray {
                while (hasMoreItems()) {
                    readItem { add(decodeJsonElement()) }
                }
            }
        }
    }

    override fun decodeJsonPrimitive(): JsonPrimitive = reader.readJsonPrimitive(strict = config.strictJsonPrimitives)

    companion object {
        private val VALUE_SUBSTRING = "value".asUtf8SubString()

        @JvmStatic
        fun compoundKeyDecoder(json: ZeroJson, parent: CompositeKeyDecoder, source: CharSequence): JsonTextDecoder {
            val pool = JsonContext.getThreadLocalPool(json)
            val ctx = JsonContext.getThreadLocalPool(json).acquire()
            return ctx.beginReadFrom(StringTextReader.startReadingFrom(source)).also {
                it.parentDecoder = parent
                it.contextPool = pool
            }
        }
    }
}

internal sealed interface CompositeKeyDecoder {
    /** direct child decoder invokes this functions right after disposing itself */
    fun compositeChildDecoderReleased()
}