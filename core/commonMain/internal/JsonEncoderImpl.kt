package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonConfiguration
import dev.dokky.zerojson.ZeroJsonEncoder
import karamel.utils.Bits32
import karamel.utils.assert
import karamel.utils.set
import karamel.utils.unsafeCast
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule

internal class JsonEncoderImpl(
    val context: JsonContext,
    private val parent: JsonEncoderImpl?,
    private val ktxJson: Json?
): ZeroJsonEncoder, CompositeEncoder, AutoCloseable {
    var zDescriptor: ZeroJsonDescriptor = ZeroJsonDescriptor.NOP
    private var elementIndex: Int = 0
    private var discriminatorKey: String? = null
    private var discriminatorValue: String? = null
    private var discriminatorPresent = false
    private var inlineRootEncoder: JsonEncoderImpl = this
    private var collectionSize: Int = DEFAULT_COLLECTION_INITIAL_CAPACITY
    private var flags = Bits32<Unit>()

    fun init(
        zDescriptor: ZeroJsonDescriptor,
        inlineRootEncoder: JsonEncoderImpl,
        jsonInlined: Boolean,
        elementIndex: Int,
        discriminatorKey: String?,
        discriminatorValue: String?,
        discriminatorPresent: Boolean,
        insideMapKey: Boolean
    ) {
        flags = Bits32()
        this.zDescriptor = zDescriptor
        this.jsonInlined = jsonInlined
        this.elementIndex = elementIndex
        this.discriminatorKey = discriminatorKey
        this.discriminatorValue = discriminatorValue
        this.discriminatorPresent = discriminatorPresent
        this.inlineRootEncoder = inlineRootEncoder
        this.insideCompoundMapKey = insideMapKey
    }

    override fun close() {
        zDescriptor = ZeroJsonDescriptor.NOP
        flags = Bits32()
        elementIndex = 0
        discriminatorPresent = false
        collectionSize = DEFAULT_COLLECTION_INITIAL_CAPACITY
        // These references are safe to keep, because they are persisted outside of this class anyway:
        // * [discriminatorKey], [discriminatorValue]: constants in ZeroJsonDescriptor
        // * [inlineRootEncoder]: all ZeroJsonEncoderImpl instances are reused
    }

    override val zeroJson: ZeroJson get() = context.zeroJson
    override val writer: JsonWriterBase get() = context.jsonWriter
    override val serializersModule: SerializersModule get() = config.serializersModule

    private val serialDescriptor: SerialDescriptor get() = zDescriptor.serialDescriptorUnsafe
    private val config: ZeroJsonConfiguration get() = context.config

    private var unsignedNumberMode: Boolean
        get() = flags[0]
        set(value) { flags = flags.set(0, value) }

    var elementsWritten: Boolean
        get() = flags[1]
        private set(value) { flags = flags.set(1, value) }

    private var insideCompoundMapKey: Boolean
        get() = flags[2]
        set(value) { flags = flags.set(2, value) }

    private var jsonInlined: Boolean
        get() = flags[3]
        set(value) { flags = flags.set(3, value) }

    override val json: Json
        get() = ktxJson ?: throwExpectedKotlinxEndec(decoder = false)

    // encodeInlineElement(descriptor, index).encodeXxxElement(Value.serializer(), value)
    // is equivalent of:
    // encodeSerializableElement(descriptor, index, Value.serializer(), value)
    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): ZeroJsonEncoder {
        elementIndex = index
        return encodeInline(descriptor.getElementDescriptor(index))
    }

    // WARN: [descriptor] describes a serializable value class
    override fun encodeInline(descriptor: SerialDescriptor): ZeroJsonEncoder {
        unsignedNumberMode = descriptor.isUnsignedNumber
        return this
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        config.encodeDefaults

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        val isNullabilitySupported = serializer.descriptor.isNullable
        if (!isNullabilitySupported && value == null) {
            elementIndex = index
            encodeNull()
        } else {
            @Suppress("UNCHECKED_CAST")
            encodeSerializableElement(descriptor, index, serializer as SerializationStrategy<T?>, value)
        }
    }

    override fun <T : Any?> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        elementIndex = index
        encodeSerializableValue(serializer, value)
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer is AbstractPolymorphicSerializer<*>) {
            requireNotNull(value) {
                "Serializer ${serializer.descriptor} does not accept null values"
            }
            @Suppress("UNCHECKED_CAST")
            encodePolymorphic(serializer as AbstractPolymorphicSerializer<T & Any>, value)
            return
        }

        serializer.serialize(this, value)
    }

    private fun <T : Any> encodePolymorphic(serializer: AbstractPolymorphicSerializer<T>, value: T) {
        val actual = context.polymorphicDeserializerResolver.lookup(this, serializer, value)
            ?: throwSubtypeNotRegistered(value::class.nameForErrorMessage(), baseClass = serializer.baseClass)

        val actualSerializer = actual.serializer.unsafeCast<SerializationStrategy<T>>()
        val classDiscriminator = actual.base.classDiscriminator!!

        if (actual.descriptor.needWrappingIfSubclass() || actual.descriptor.kind is PolymorphicKind) {
            encodeWrappedSubClass(
                actualSerializer = actualSerializer,
                discriminatorKey = classDiscriminator,
                discriminatorValue = actual.serialName,
                value = value
            )
            return
        }

        val encoder = if (elementIndex < 0) {
            // current encoder is root
            assert { parent == null }
            zDescriptor = ZeroJsonDescriptor.ROOT
            discriminatorKey = classDiscriminator
            discriminatorValue = actual.serialName
            this
        } else {
            context.nextPolymorphicSubEncoder(
                zDescriptor = actual.descriptor,
                discriminatorKey = classDiscriminator,
                discriminatorValue = actual.serialName,
                discriminatorPresent = actual.discriminatorPresent
            )
        }

        actualSerializer.serialize(encoder, value)

        context.releaseEncoder(encoder)
    }

    private fun <T : Any> encodeWrappedSubClass(
        actualSerializer: SerializationStrategy<T>,
        discriminatorKey: String,
        discriminatorValue: String,
        value: T
    ) {
        val insideMapKey = beforeStructure(parent = this, actualSerializer.descriptor)
        val encoder = context.nextEncoder(
            zDescriptor = ZeroJsonDescriptor.POLYMORPHIC_VALUE_WRAPPER,
            inlineRootEncoder = null,
            jsonInlined = false,
            elementIndex = 1,
            insideMapKey = insideMapKey
        )
        writer.writeString(discriminatorKey)
        writer.colon()
        writer.writeString(discriminatorValue)
        encoder.elementsWritten = true

        encoder.encodeSerializableElement(encoder.serialDescriptor, 1, actualSerializer, value)

        writer.endObject()
        if (insideMapKey) writer.endString()
        context.releaseEncoder(encoder)
    }

    private fun beforeStructure(parent: JsonEncoderImpl?, descriptor: SerialDescriptor): Boolean {
        val insideMapKey = beforeElementStructure(parent)
        if (insideMapKey) config.throwIfStructuredKeysDisabled(descriptor)

        when (descriptor.kind) {
            StructureKind.LIST -> writer.beginArray(collectionSize)
            else -> writer.beginObject(collectionSize)
        }
        collectionSize = DEFAULT_COLLECTION_INITIAL_CAPACITY

        return insideMapKey
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        this.collectionSize = collectionSize
        return beginStructure(descriptor)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val childDescriptor = getZDescriptor(descriptor, context)
        val descriptor = childDescriptor.serialDescriptorUnsafe

        discriminatorKey?.let { dk ->
            this.zDescriptor = childDescriptor
            insideCompoundMapKey = beforeStructure(parent, descriptor)
            if (!discriminatorPresent) {
                writer.writeString(dk)
                writer.colon()
                writer.writeString(discriminatorValue!!)
            }
            elementsWritten = true
            discriminatorKey = null
            return this
        }

        val jsonInlined = elementIndex >= 0 && zDescriptor.isElementJsonInline(elementIndex)
        val insideMapKey = !jsonInlined && beforeStructure(parent = this, descriptor)
        return if (elementIndex < 0) {
            assert { parent == null }
            zDescriptor = childDescriptor // current encoder is root
            this
        } else {
            context.nextEncoder(
                zDescriptor = childDescriptor,
                inlineRootEncoder = inlineRootEncoder.takeIf<JsonEncoderImpl> { jsonInlined },
                jsonInlined = jsonInlined,
                elementIndex = 0,
                insideMapKey = insideMapKey
            )
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        debugAssert { zDescriptor.check(descriptor) }

        if (!jsonInlined) {
            if (zDescriptor.kind == StructureKind.LIST) writer.endArray() else writer.endObject()
            afterElementStructure()
        }

        // non-null discriminatorValue means this encoder was acquired in encodePolymorphic()
        if (discriminatorValue == null) context.releaseEncoder(this)
    }

    private fun beforeElementStructure(parent: JsonEncoderImpl?): Boolean {
        var insideMapKey = false
        if (parent != null && parent.elementIndex >= 0) {
            insideMapKey = parent.beforeElement()
            if (insideMapKey) writer.beginString()
        }
        return insideMapKey
    }

    private fun afterElementStructure() {
        if (insideCompoundMapKey) {
            writer.endString()
            writer.colon()
        }
    }

    override fun encodeNull() {
        encodeNull(force = false)
    }

    private fun encodeNull(force: Boolean) {
        val index = elementIndex
        if (index < 0) {
            writer.writeNull()
            unsignedNumberMode = false
            return
        }

        val flags = zDescriptor.kindFlags

        if (!force &&
            flags.isJsonObject &&
            !config.explicitNulls &&
            serialDescriptor.kind != StructureKind.MAP)
            return

        writeCommaBeforeElementIfNeeded(flags, index)

        val isKey = if (flags.isClassLike) {
            writer.writeKey(zDescriptor.getElementName(index = index))
            writer.colon()
            false
        } else {
            flags.isMapKey(elementIndex = index)
        }

        if (isKey) {
            writer.writeString("null")
            writer.colon()
        } else {
            writer.writeNull()
        }

        unsignedNumberMode = false
    }

    override fun encodeBoolean(value: Boolean) = writeValue {
        writer.writeBoolean(value)
    }

    override fun encodeByte(value: Byte) = writeValue {
        if (unsignedNumberMode) {
            writer.writeNumber(value.toUByte())
        } else {
            writer.writeNumber(value)
        }
    }

    override fun encodeShort(value: Short) = writeValue {
        if (unsignedNumberMode) {
            writer.writeNumber(value.toUShort())
        } else {
            writer.writeNumber(value)
        }
    }

    override fun encodeChar(value: Char) = writeValue(quoted = true) {
        writer.writeString(value)
    }

    override fun encodeInt(value: Int) = writeValue {
        if (unsignedNumberMode) {
            writer.writeNumber(value.toUInt())
        } else {
            writer.writeNumber(value)
        }
    }

    override fun encodeLong(value: Long) = writeValue {
        if (unsignedNumberMode) {
            writer.writeNumber(value.toULong())
        } else {
            writer.writeNumber(value)
        }
    }

    override fun encodeFloat(value: Float) = writeValue {
        writer.writeNumber(value)
    }

    override fun encodeDouble(value: Double) = writeValue {
        writer.writeNumber(value)
    }

    override fun encodeString(value: String) = writeValue(quoted = true) {
        writer.writeString(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    /** @return `true` if closing double quotes (") is needed */
    private fun beforeElement(): Boolean {
        val index = elementIndex
        val flags = zDescriptor.kindFlags

        assert { index >= 0 }

        writeCommaBeforeElementIfNeeded(flags = flags, index = index)

        return if (flags.isClassLike) {
            writer.writeKey(zDescriptor.getElementName(index))
            writer.colon()
            false
        } else {
            flags.isMapKey(elementIndex = index)
        }
    }

    private fun JsonEncoderImpl.writeCommaBeforeElementIfNeeded(flags: SerialKindFlags, index: Int) {
        if (inlineRootEncoder.elementsWritten) {
            if (flags.needCommaBeforeElement(index)) writer.comma()
        } else {
            inlineRootEncoder.elementsWritten = true
        }
    }

    private inline fun writeValue(quoted: Boolean = false, body: () -> Unit) {
        writeElementTemplate(quoted = quoted, body)
        unsignedNumberMode = false
    }

    private inline fun writeElement(index: Int, quoted: Boolean = false, body: () -> Unit) {
        elementIndex = index
        writeElementTemplate(quoted = quoted, body)
    }

    private inline fun writeElementTemplate(quoted: Boolean = false, body: () -> Unit) {
        var isKey = false

        if (elementIndex >= 0) {
            isKey = beforeElement()
            if (isKey && !quoted) writer.beginString()
        }

        body()

        if (isKey) writer.apply {
            if (!quoted) endString()
            colon()
        }
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        writeElement(index) { writer.writeBoolean(value) }
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        writeElement(index) { writer.writeNumber(value) }
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        writeElement(index) { writer.writeNumber(value) }
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        writeElement(index, quoted = true) { writer.writeString(value) }
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        writeElement(index) { writer.writeNumber(value) }
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        writeElement(index) { writer.writeNumber(value) }
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        writeElement(index) { writer.writeNumber(value) }
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        writeElement(index) { writer.writeNumber(value) }
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        writeElement(index, quoted = true) { writer.writeString(value) }
    }

    override fun toString() = "ZeroJsonEncoder(${serialDescriptor.serialName})"

    override fun encodeJsonElement(element: JsonElement) {
        when(element) {
            is JsonArray -> {
                checkJsonPrimitiveIsNotSubClass(element)
                insideCompoundMapKey = beforeElementStructure(parent = this)
                if (insideCompoundMapKey) config.throwIfStructuredKeysDisabled(JsonArray.serializer().descriptor)
                writer.write(element, skipNullKeys = false) // todo skipNullKeys=true as an option
                afterElementStructure()
            }
            is JsonObject -> {
                insideCompoundMapKey = beforeElementStructure(parent = if (discriminatorKey != null) parent else this)
                if (insideCompoundMapKey) config.throwIfStructuredKeysDisabled(JsonObject.serializer().descriptor)
                // WARN! it is necessary to check discriminatorKey != null first,
                // otherwise zDescriptor will point to the parent structure
                val dk = discriminatorKey
                if (dk == null || discriminatorPresent) {
                    writer.write(element, skipNullKeys = false)
                } else {
                    writer.write(element, dk, discriminatorValue, skipNullKeys = false)
                }
                elementsWritten = true
                discriminatorKey = null
                afterElementStructure()
            }
            is JsonPrimitive -> {
                checkJsonPrimitiveIsNotSubClass(element)
                if (element == JsonNull) encodeNull(force = true) else {
                    writeValue(quoted = element.isString) {
                        writer.write(element, skipNullKeys = false)
                    }
                }
            }
        }
    }

    private fun checkJsonPrimitiveIsNotSubClass(element: JsonElement) {
        if (discriminatorKey != null) {
            throwJsonElementCanNotBeSerializedPolymorphically(element, zDescriptor)
        }
    }

    private fun throwJsonElementCanNotBeSerializedPolymorphically(
        element: JsonElement,
        descriptor: ZeroJsonDescriptor
    ): Nothing {
        throw SerializationException(
            "Class with serial name ${descriptor.serialName} " +
            "cannot be serialized polymorphically because it is represented as ${element::class.simpleName}. " +
            "Make sure that its JsonTransformingSerializer returns JsonObject, " +
            "so class discriminator can be added to it"
        )
    }

    private companion object {
        const val DEFAULT_COLLECTION_INITIAL_CAPACITY = 8
    }
}

internal fun getZDescriptor(descriptor: SerialDescriptor, context: JsonContext): ZeroJsonDescriptor =
    when (descriptor.kind) {
        is StructureKind.MAP -> when {
            descriptor.isMapWithStructuredKey(context.config) -> ZeroJsonDescriptor.LIST
            else -> ZeroJsonDescriptor.MAP
        }
        is StructureKind.LIST -> ZeroJsonDescriptor.LIST
        else -> context.descriptorCache.getOrCreateUnsafe(descriptor)
    }