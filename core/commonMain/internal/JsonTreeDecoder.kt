package dev.dokky.zerojson.internal

import dev.dokky.zerojson.StructuredMapKeysMode
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonConfiguration
import dev.dokky.zerojson.ZeroJsonDecodingException
import io.kodec.text.SimpleSubString
import karamel.utils.assert
import karamel.utils.unsafeCast
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

internal class JsonTreeDecoder(
    private val context: JsonContext,
    private val parent: JsonTreeDecoder?,
    private val ktxJson: Json?
): AbstractDecoder(), ZeroJsonBaseDecoder, CompositeDecoder, AutoCloseable, CompositeKeyDecoder {
    var descriptor: ZeroJsonDescriptor = ZeroJsonDescriptor.NOP
    private var inlineParentDecoder: JsonTreeDecoder = this
    var parentElement: JsonElement = JsonNull
    // todo JsonElement -> Any. Write primitive content directly if possible and type-check on every read
    var element: JsonElement = JsonNull
    var elementIndex: Int = -1
    private var discriminatorKey: String = ""
    private var lookupResult: PolymorphicSerializerCache.DeserializerLookupResult? = null
    private var polySubClassWrapping = false
    private var inlineOffset: Int = 0

    private enum class Mode { Normal, Unsigned, Null }
    private var mode = Mode.Normal
    private var prepareInlineDecodingOffset = 0

    private var mapIterator: Iterator<Map.Entry<String, JsonElement>>? = null
    private var mapEntryValue: JsonElement? = null
    var currentKey: String? = null
        private set

    private var compoundKeyDecoder: JsonTextDecoder? = null
    private var acquiredContext: JsonContext? = null

    override val zeroJson: ZeroJson get() = context.zeroJson
    override val serializersModule: SerializersModule
        get() = config.serializersModule
    private val config: ZeroJsonConfiguration get() = context.config

    override fun close() {
        descriptor = ZeroJsonDescriptor.NOP
        inlineParentDecoder = this
        parentElement = JsonNull
        element = JsonNull
        elementIndex = -1
        discriminatorKey = ""
        lookupResult = null
        polySubClassWrapping = false
        inlineOffset = 0

        mode = Mode.Normal
        prepareInlineDecodingOffset = 0

        mapIterator = null
        mapEntryValue = null
        currentKey = null

        compoundKeyDecoder?.let {
            it.close()
            compoundKeyDecoder = null
        }

        acquiredContext?.let {
            context.ownerPool.release(it)
            acquiredContext = null
        }
    }

    override val json: Json
        get() = ktxJson ?: throwExpectedKotlinxEndec(decoder = true)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val mapKeysMode = config.structuredMapKeysMode
        var zDescriptor = lookupResult?.descriptor ?: run {
            if (mapKeysMode != StructuredMapKeysMode.LIST && isDecodingMapKey()) {
                return compoundKeyDecoder(element, descriptor).beginStructure(descriptor)
            }
            this.descriptor.getElementDescriptor(elementIndex, context.descriptorCache, descriptor)
        }

        if (mapKeysMode == StructuredMapKeysMode.LIST &&
            zDescriptor.kind == StructureKind.MAP &&
            zDescriptor.getElementDescriptor(0, context.descriptorCache, descriptor).kindFlags.isCompound)
        {
            zDescriptor = ZeroJsonDescriptor.LIST
        }

        val decoder = subDecoder(zDescriptor)
        if (DebugMode) decoder.assertEqualDescriptors(descriptor)

        if (decoder.inlineOffset == 0) {
            if (zDescriptor.kind == StructureKind.MAP) {
                decoder.mapIterator = element.jsonObjectOrFail().iterator()
            } else if (zDescriptor.kind != StructureKind.LIST) {
                context.treeDecodingStack.enter(zDescriptor)
                val discriminatorPresent = lookupResult.isDiscriminatorPresent()

                for (entry in element.jsonObjectOrFail()) {
                    if (!discriminatorPresent && entry.key == discriminatorKey) continue

                    val element = zDescriptor.getElementInfoByName(entry.key)
                    if (element.isUnknown) {
                        if (zDescriptor.ignoreUnknownKeys) continue
                        unknownKeyError(entry.key)
                    }
                    debugAssert { element.isValid }

                    context.treeDecodingStack.tryMarkInlineSite(element)

                    if (element == zDescriptor.inlineMapElement) {
                        context.treeDecodingStack.addMapEntry(entry)
                    } else {
                        context.treeDecodingStack.putElement(element.index, entry)
                    }
                }
            }
        } else if (zDescriptor.kind != descriptor.kind) {
            throwInlineMapCompositeKeysAreNotSupported()
        }

        lookupResult = null
        discriminatorKey = ""
        return decoder
    }

    override fun compositeChildDecoderReleased() {
        compoundKeyDecoder = null
    }

    private fun compoundKeyDecoder(element: JsonElement, keyDescriptor: SerialDescriptor): JsonTextDecoder {
        config.throwIfStructuredKeysDisabled(keyDescriptor)
        if (compoundKeyDecoder != null) {
            error("attempt to decoder composite key, but previous composite decoding is not finished with endStructure()")
        }
        return JsonTextDecoder.compoundKeyDecoder(zeroJson, parent = this, source = element.jsonPrimitiveOrFail().content).also {
            compoundKeyDecoder = it
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (DebugMode) assertEqualDescriptors(descriptor)

        if (!this.descriptor.kindFlags.isCollection && inlineOffset == 0) {
            context.treeDecodingStack.leave(this.descriptor)
        }

        context.releaseTreeDecoder(this)
    }

    private fun assertEqualDescriptors(descriptor: SerialDescriptor) {
        if (!this.descriptor.check(descriptor)) {
            error("descriptor mismatch: ${descriptor.serialName} != ${this.descriptor.serialName}")
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = when(this.descriptor.kind) {
        StructureKind.LIST -> decodeListIndex(descriptor)
        StructureKind.MAP -> if (inlineOffset > 0) decodeInlineMapIndex(descriptor) else decodeMapIndex(descriptor)
        else -> decodePropertyIndex(descriptor)
    }

    private fun decodeListIndex(descriptor: SerialDescriptor): Int {
        val idx = elementIndex + 1
        val array = parentElement.jsonArray
        return if (idx >= array.size) {
            if (descriptor.kind != StructureKind.LIST) checkIfMapIsMissingValue(prevIdx = elementIndex)
            element = JsonNull
            CompositeDecoder.DECODE_DONE
        } else {
            element = array[idx]
            elementIndex = idx
            idx
        }
    }

    private fun decodeMapIndex(serialDescriptor: SerialDescriptor): Int {
        val newIndex = elementIndex + 1
        while (true) {
            element = if (newIndex and 1 == 1) mapEntryValue!! else {
                val i = mapIterator ?: error("decodeElementIndex() must be called in between beginStructure/endStructure")
                if (!i.hasNext()) {
                    mapIterator = null
                    mapEntryValue = null
                    currentKey = null
                    element = JsonNull
                    elementIndex = newIndex
                    return CompositeDecoder.DECODE_DONE
                }
                val entry = i.next()
                currentKey = entry.key
                mapEntryValue = entry.value
                if (config.coerceInputValues && trySkipMapKey(serialDescriptor, entry.value)) continue
                JsonPrimitive(entry.key)
            }

            elementIndex = newIndex
            return newIndex
        }
    }

    private fun decodeInlineMapIndex(serialDescriptor: SerialDescriptor): Int {
        val newIndex = elementIndex + 1

        if (newIndex and 1 == 1) { // decoding value
            element = mapEntryValue!!
            elementIndex = newIndex
            return newIndex
        }

        while (true) {
            val entry = context.treeDecodingStack.removeMapEntry() ?: run {
                mapEntryValue = null
                currentKey = null
                element = JsonNull
                elementIndex = newIndex
                return CompositeDecoder.DECODE_DONE
            }
            currentKey = entry.key
            mapEntryValue = entry.value
            if (config.coerceInputValues && trySkipMapKey(serialDescriptor, entry.value)) continue
            element = JsonPrimitive(entry.key)
            elementIndex = newIndex
            return newIndex
        }
    }

    private fun trySkipMapKey(
        serialDescriptor: SerialDescriptor,
        value: JsonElement
    ): Boolean {
        val valueDescriptor = serialDescriptor.getElementDescriptor(1)
        if (valueDescriptor.kind != SerialKind.ENUM) return false
        if (value == JsonNull) return true
        if (!value.isString()) return false
        return context.descriptorCache
            .getOrCreateUnsafe(valueDescriptor)
            .getElementInfoByName(value.content)
            .isUnknown
    }

    private fun decodePropertyIndex(descriptor: SerialDescriptor): Int {
        while (true) {
            val idx = elementIndex + 1
            if (idx >= descriptor.elementsCount) {
                element = JsonNull
                elementIndex = CompositeDecoder.DECODE_DONE
                return CompositeDecoder.DECODE_DONE
            }
            elementIndex = idx

            if (this.descriptor.isElementJsonInline(idx)) {
                val result = decodeInlinePropertyIndex(descriptor, idx, inlineOffset)
                if (result < 0) continue else return result
            }

            val absoluteIndex = inlineOffset + idx

            val element = context.treeDecodingStack.getElementValue(absoluteIndex)
            if (element == null) {
                if (descriptor.canElementBeImplicitlyNull(idx)) {
                    mode = Mode.Null
                    return idx
                }
                continue
            }

            currentKey = context.treeDecodingStack.getElementKey(absoluteIndex)

            if (config.coerceInputValues) {
                when(tryCoercePropertyValue(element, idx, descriptor)) {
                    CoerceResult.RETURN_IDX -> return idx
                    CoerceResult.SKIP_ELEMENT -> continue
                    CoerceResult.NONE -> {}
                }
            }

            this.element = element
            return idx
        }
    }

    private enum class CoerceResult { RETURN_IDX, SKIP_ELEMENT, NONE }

    private fun tryCoercePropertyValue(element: JsonElement, idx: Int, descriptor: SerialDescriptor): CoerceResult {
        val elementDescriptor = this.descriptor.getElementDescriptor(idx, context.descriptorCache, descriptor)

        if (element === JsonNull) {
            if (elementDescriptor.isNullable) return CoerceResult.NONE
            if (descriptor.isElementOptional(idx)) return CoerceResult.SKIP_ELEMENT
        }

        if (elementDescriptor.kind == SerialKind.ENUM &&
            (descriptor.isElementOptional(idx) || (elementDescriptor.isNullable && !zeroJson.configuration.explicitNulls)) &&
            elementDescriptor.getElementInfoByName(element.string).isUnknown)
        {
            if (descriptor.isElementOptional(idx)) return CoerceResult.SKIP_ELEMENT else {
                mode = Mode.Null
                return CoerceResult.RETURN_IDX
            }
        }

        return CoerceResult.NONE
    }

    /** @return -1 if the property should be skipped */
    private fun decodeInlinePropertyIndex(
        descriptor: SerialDescriptor,
        index: Int,
        inlineOffset: Int
    ): Int {
        val elementAbsoluteIndex = inlineOffset + index
        val childDescriptor = descriptor.getElementDescriptor(index)
        val canBeImplicitNull = childDescriptor.isNullable || config.explicitNulls

        prepareInlineDecodingOffset = inlineParentDecoder.descriptor.getChildElementsOffset(elementAbsoluteIndex)

        if (!descriptor.isElementOptional(index) && !canBeImplicitNull) {
            element = JsonTrue
            return index
        }

        val isElementPresent = context.treeDecodingStack.isElementPresent(elementAbsoluteIndex) ||
            anyElementExistsInInlineSubTree(descriptor, index, prepareInlineDecodingOffset)

        if (!isElementPresent) when {
            canBeImplicitNull -> mode = Mode.Null
            else -> {
                prepareInlineDecodingOffset = 0
                return -1 // the property is optional, we must skip it
            }
        }

        element = JsonTrue
        return index
    }

    private fun anyElementExistsInInlineSubTree(
        serialDesc: SerialDescriptor,
        elementIndex: Int,
        childElementsOffset: Int
    ): Boolean = anyElementExistsInInlineSubTreeTemplate(
        rootDesc = inlineParentDecoder.descriptor,
        serialDesc = serialDesc,
        elementIndex = elementIndex,
        childElementsOffset = childElementsOffset,
        isElementPresent = { absIdx -> context.treeDecodingStack.isElementPresent(absIdx) },
        markElementPresent = { absIdx -> context.treeDecodingStack.markElementIsPresent(absIdx) },
        callRecursive = { _, serialDesc, elementIndex, childElementsOffset ->
            anyElementExistsInInlineSubTree(serialDesc, elementIndex, childElementsOffset)
        }
    )

    private fun SerialDescriptor.canElementBeImplicitlyNull(idx: Int): Boolean =
        !zeroJson.configuration.explicitNulls &&
        !isElementOptional(idx) &&
        getElementDescriptor(idx).isNullable

    private val tempSubString = SimpleSubString()

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        var deserializer = deserializer
        var jsonElementType: KClass<out JsonElement>? = null

        if (deserializer is AbstractPolymorphicSerializer<T>) {
            assert { prepareInlineDecodingOffset == 0 }

            if (config.structuredMapKeysMode != StructuredMapKeysMode.LIST && isDecodingMapKey()) {
                return compoundKeyDecoder(element, deserializer.descriptor)
                    .decodeSerializableValue(deserializer)
            }

            if (polySubClassWrapping) {
                // polymorphic value subclass is polymorphic itself
                val decoder = valueSubClassDecoder()
                val result = decoder.decodeSerializableValue(deserializer)
                context.releaseTreeDecoder(decoder)
                return result
            }

            deserializer = preparePolymorphicDeserialization(
                deserializer.unsafeCast<AbstractPolymorphicSerializer<T & Any>>()
            )
        } else {
            jsonElementType = jsonElementSerializerToClass[deserializer]
        }

        polySubClassWrapping = polySubClassWrapping && lookupResult!!.descriptor.needWrappingIfSubclass()

        val decoder = if (polySubClassWrapping) valueSubClassDecoder() else this
        val result = when {
            jsonElementType != null -> decodeJsonElement(jsonElementType, decoder)
            else -> deserializer.deserialize(decoder)
        }
        if (this !== decoder) context.releaseTreeDecoder(decoder)
        return result
    }

    private fun <T> decodeJsonElement(expectedType: KClass<out JsonElement>, decoder: JsonTreeDecoder): T {
        if (!expectedType.isInstance(decoder.element)) {
            throwInvalidElementType(expectedType, decoder.element)
        }

        @Suppress("UNCHECKED_CAST")
        return decoder.element as T
    }

    private val jsonElementSerializerToClass = buildMap<DeserializationStrategy<*>, KClass<out JsonElement>>(4) {
        put(JsonPrimitive.serializer(), JsonPrimitive::class)
        put(JsonArray.serializer(), JsonArray::class)
        put(JsonObject.serializer(), JsonObject::class)
    }

    private fun <T: Any> preparePolymorphicDeserialization(
        deserializer: AbstractPolymorphicSerializer<T>
    ): DeserializationStrategy<T> {
        val baseDescriptor = descriptor.getElementDescriptor(
            index = elementIndex,
            cache = context.descriptorCache,
            elementDescriptor = deserializer.descriptor
        )
        discriminatorKey = baseDescriptor.classDiscriminator!!

        val discriminatorValue = element.jsonObjectOrFail()[discriminatorKey]
        val lookupResult = if (discriminatorValue == null || discriminatorValue === JsonNull) {
            context.polymorphicDeserializerResolver.lookupDefaultDeserializer(deserializer, baseDescriptor)
                ?: missingDiscriminator(deserializer)
        } else {
            if (discriminatorValue !is JsonPrimitive || (!config.isLenient && !discriminatorValue.isString)) {
                throw ZeroJsonDecodingException("expected string")
            }
            val discriminator = discriminatorValue.content
            tempSubString.set(discriminator, hashCode = discriminator.hashCode())
            context.polymorphicDeserializerResolver
                .lookup(this, deserializer, baseDescriptor, tempSubString)
                ?: throwUnknownSubTypeError(deserializer, subType = discriminator)
        }

        val actualDeserializer = lookupResult.deserializer.unsafeCast<DeserializationStrategy<T>>()
        this.lookupResult = lookupResult
        polySubClassWrapping = true

        return actualDeserializer
    }

    private fun isDecodingMapKey(): Boolean = this.descriptor.kindFlags.isMapKey(elementIndex)

    private fun subDecoder(descriptor: ZeroJsonDescriptor): JsonTreeDecoder {
        val parentElement = if (prepareInlineDecodingOffset == 0) element else parentElement
        if (parentElement is JsonPrimitive && descriptor != ZeroJsonDescriptor.ROOT) {
            throwExpectedStructure(structureDescriptor = descriptor, actualElement = parentElement)
        }
        return context.nextTreeDecoder(descriptor, parentElement).also {
            it.inlineParentDecoder = if (prepareInlineDecodingOffset == 0) it else inlineParentDecoder
            it.inlineOffset = prepareInlineDecodingOffset
            prepareInlineDecodingOffset = 0
        }
    }

    override fun decodeNotNullMark(): Boolean {
        if (mode == Mode.Null) return false
        return element != JsonNull
    }

    override fun decodeNull(): Nothing? {
        if (mode == Mode.Null) mode = Mode.Normal else require(element == JsonNull)
        return null
    }

    override fun decodeJsonElement(): JsonElement = when(val e = element) {
        is JsonObject -> decodeJsonObject(e)
        else -> e
    }

    override fun decodeJsonObject(): JsonObject {
        val element = element.jsonObjectOrFail()
        return decodeJsonObject(element)
    }

    private fun decodeJsonObject(element: JsonObject): JsonObject {
        val lr = lookupResult ?: return element
        val result = if (lr.discriminatorPresent) element else JsonObject(element - discriminatorKey)
        discriminatorKey = ""
        lookupResult = null
        return result
    }

    override fun decodeJsonArray(): JsonArray = element.jsonArrayOrFail()

    override fun decodeJsonPrimitive(): JsonPrimitive = element.jsonPrimitiveOrFail()

    private fun valueSubClassDecoder(): JsonTreeDecoder {
        polySubClassWrapping = false

        val value = element.jsonObjectOrFail()["value"] ?: run {
            if (config.explicitNulls || !lookupResult!!.descriptor.isNullable) missingPolyValueField()
            JsonNull
        }

        return subDecoder(ZeroJsonDescriptor.POLYMORPHIC_VALUE_WRAPPER).also {
            it.elementIndex = 0
            it.mode = Mode.Unsigned
            it.element = value
            mode = Mode.Normal
            discriminatorKey = ""
            lookupResult = null
        }
    }

    private fun missingPolyValueField(): Nothing = throw MissingFieldException(
        missingFields = listOf("value"),
        message = "Field 'value' is required for value class with serial name '${lookupResult?.descriptor?.serialName}' " +
                "because it is deserialized as a subclass of '${parent?.descriptor?.serialName}'",
        cause = null
    )

    private inline fun <R> decodePrimitive(expectedType: String, body: (JsonReaderImpl) -> R): R {
        val primitive = element.jsonPrimitiveOrFail()
        if (!primitive.isString && isDecodingMapKey() && !config.isLenient) {
            throwExpectedString()
        }

        val pool = context.ownerPool
        val ctx = pool.acquire()
        acquiredContext = ctx

        val input = ctx.stringInput.also { it.startReadingFrom(primitive.content) }
        val reader = ctx.reader.also { it.input = input }
        val result = body(reader)
        if (reader.nextCodePoint != -1) expectedType(expectedType)

        acquiredContext = null
        pool.release(ctx)
        return result
    }

    private inline fun <R> decodeStringPrimitive(body: JsonTreeDecoder.(String) -> R): R {
        val primitive = element.jsonPrimitiveOrFail()
        if (!primitive.isString && !(config.isLenient && primitive !== JsonNull)) throwExpectedString()
        return this.body(primitive.content)
    }

    override fun decodeBoolean(): Boolean = decodePrimitive("boolean") {
        it.readBoolean()
    }

    override fun decodeByte(): Byte = decodePrimitive("integer") { reader ->
        if (mode != Mode.Unsigned) reader.readByte(skipWhitespace = false) else {
            mode = Mode.Normal
            reader.readUInt(8, quotes = false).toByte()
        }
    }

    override fun decodeShort(): Short = decodePrimitive("integer") { reader ->
        if (mode != Mode.Unsigned) reader.readShort(skipWhitespace = false) else {
            mode = Mode.Normal
            reader.readUInt(16, quotes = false).toShort()
        }
    }

    override fun decodeInt(): Int = decodePrimitive("integer") { reader ->
        if (mode != Mode.Unsigned) reader.readInt(skipWhitespace = false) else {
            mode = Mode.Normal
            reader.readUInt(32, quotes = false).toInt()
        }
    }

    override fun decodeLong(): Long = decodePrimitive("integer") {
        if (mode != Mode.Unsigned) it.readLong(skipWhitespace = false) else {
            mode = Mode.Normal
            it.input.readJsonUnsingedLong().toLong()
        }
    }

    override fun decodeChar(): Char = decodeStringPrimitive { it.firstOrNull() }
        ?: throw ZeroJsonDecodingException("expected a single character")

    override fun decodeFloat(): Float = decodePrimitive("number") { it.readFloat() }

    override fun decodeDouble(): Double = decodePrimitive("number") { it.readDouble() }

    override fun decodeString(): String = decodeStringPrimitive { it }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val entryName = decodeString()
        val elementInfo = descriptor
            .getElementDescriptor(elementIndex, context.descriptorCache, enumDescriptor)
            .getElementInfoByName(entryName)
        if (elementInfo.isUnknown) unknownEnumEntry(entryName)
        return elementInfo.index
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        if (descriptor.isUnsignedNumber) mode = Mode.Unsigned
        return this
    }

    private fun throwExpectedStructure(actualElement: JsonPrimitive, structureDescriptor: ZeroJsonDescriptor): Nothing {
        val expectedType = if (structureDescriptor.kind == StructureKind.LIST) "list" else "object"
        throw ZeroJsonDecodingException("expected $expectedType, got $actualElement")
    }

    private fun throwInvalidElementType(expectedType: KClass<out JsonElement>, actual: JsonElement): Nothing {
        val type = when (expectedType) {
            JsonArray::class -> "array"
            JsonObject::class -> "object"
            else -> "element"
        }
        throw ZeroJsonDecodingException("expected $type, got ${actual.readableType()}")
    }

    private fun expectedType(expectedType: String): Nothing =
        throw ZeroJsonDecodingException("expected $expectedType")

    private fun throwExpectedString(): Nothing {
        throw ZeroJsonDecodingException("expected string, got ${element.readableType()}")
    }

    private fun JsonElement.jsonObjectOrFail(): JsonObject = this as? JsonObject
        ?: throw ZeroJsonDecodingException("expected object, got ${readableType()}")

    private fun JsonElement.jsonArrayOrFail(): JsonArray = this as? JsonArray
        ?: throw ZeroJsonDecodingException("expected array, got ${readableType()}")

    private fun JsonElement.jsonPrimitiveOrFail(): JsonPrimitive = this as? JsonPrimitive
        ?: throw ZeroJsonDecodingException("unexpected ${readableType()}")

    private val JsonElement.string: String get() {
        if (!isString()) throw ZeroJsonDecodingException("expected string, got ${readableType()}")
        return content
    }

    @OptIn(ExperimentalContracts::class)
    private fun JsonElement.isString(): Boolean {
        contract {
            returns() implies (this@isString is JsonPrimitive)
        }
        return this is JsonPrimitive && (this.isString || config.isLenient)
    }
}

private fun JsonElement.readableType(): String = when(this) {
    is JsonPrimitive -> this.toString()
    is JsonObject -> "object"
    is JsonArray -> "array"
}

private val JsonTrue = JsonPrimitive(true)