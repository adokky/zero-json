package dev.dokky.zerojson.internal

import androidx.collection.MutableIntList
import androidx.collection.MutableObjectIntMap
import dev.dokky.zerojson.JsonInline
import dev.dokky.zerojson.MaterializedDiscriminator
import dev.dokky.zerojson.ZeroJsonConfiguration
import io.kodec.StringsASCII
import io.kodec.buffers.ArrayBuffer
import io.kodec.buffers.InternalBuffersApi
import io.kodec.buffers.asBuffer
import io.kodec.text.*
import karamel.utils.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.NothingSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

internal class ZeroJsonDescriptor private constructor(
    serialDescriptorUnsafe: SerialDescriptor,
    private var elements: Array<ZeroJsonDescriptor?>,
    private var elementNames: Array<String>?,
    // key = AbstractSubString | String
    private var elementByName: MutableObjectIntMap<Any>,
    private var flags: Bits32<Unit>,
    inlineMapElement: ElementInfo,
    totalElementCount: Int,
    private var elementOffsets: ShortArray?,
    val kindFlags: SerialKindFlags,
    val classDiscriminator: String?,
    val classDiscriminatorUtf8Encoded: AbstractSubString?,
    val classDiscriminatorSubString: SimpleSubString?
) {
    /**
     * Descriptors with kinds [StructureKind.MAP]/[StructureKind.LIST] are not preserved.
     * Interned version with invalid element info is used instead.
     *
     * This property does NOT check if current descriptor
     * contains only relevant info and can NOT be used safely anywhere.
     */
    var serialDescriptorUnsafe: SerialDescriptor = serialDescriptorUnsafe
        private set

    var inlineMapElement: ElementInfo = inlineMapElement
        private set

    // initial value -1 is a marker of uninitialized descriptor
    var totalElementCount: Int = totalElementCount
        private set

    // properties without backing field

    val serialDescriptor: SerialDescriptor get() {
        debugAssert { this != MAP && this != LIST }
        return serialDescriptorUnsafe
    }

    val caseInsensitiveEnum: Boolean            get() = flags[FLAG_CASE_INSENSITIVE]
    val allNamesAreAscii: Boolean               get() = flags[FLAG_NAMES_ARE_ASCII]
    val allowMaterializedDiscriminator: Boolean get() = flags[FLAG_MATERIALIZED_DISCRIMINATOR]
    val hasJsonInlineElements: Boolean          get() = flags[FLAG_HAS_INLINE_ELEMENTS]
    val ignoreUnknownKeys: Boolean              get() = flags[FLAG_IGNORE_UNKNOWN_KEYS]

    val hasInlineMapElement: Boolean get() = inlineMapElement.isValid
    val isInitialized: Boolean get() = totalElementCount >= 0

    fun copyAsNullable(nullableDescriptor: SerialDescriptor = serialDescriptorUnsafe.nullable): ZeroJsonDescriptor {
        require(isInitialized) { "attempt to copy uninitialized descriptor" }
        assert { nullableDescriptor.isNullable }
        checkEqualSerialNames(nullableDescriptor.serialName, serialName)
        return ZeroJsonDescriptor(
            serialDescriptorUnsafe = nullableDescriptor,
            elements = elements,
            elementNames = elementNames,
            elementByName = elementByName,
            flags = flags,
            inlineMapElement = inlineMapElement,
            totalElementCount = totalElementCount,
            elementOffsets = elementOffsets,
            kindFlags = kindFlags,
            classDiscriminator = classDiscriminator,
            classDiscriminatorUtf8Encoded = classDiscriminatorUtf8Encoded,
            classDiscriminatorSubString = classDiscriminatorSubString
        )
    }

    fun initElements(
        elements: Array<ZeroJsonDescriptor?>,
        useAlternativeNames: Boolean,
        namingStrategy: JsonNamingStrategy?
    ) {
        assert { !isInitialized }

        this.elements = elements

        if (kind != SerialKind.ENUM && !kind.isClassLike()) {
            totalElementCount = 0
            return
        }

        if (serialDescriptorUnsafe.annotations.findFirstInstanceOfOrNull<MaterializedDiscriminator>() != null) {
            flags += FLAG_MATERIALIZED_DISCRIMINATOR
        }

        // missingValue = ElementInfo.UNKNOWN_NAME.asInt
        elementByName = MutableObjectIntMap(initialCapacity = elements.size)

        val elementOffsets = if (kind.isClassLike()) {
            MutableIntList(elements.size.coerceAtLeast(16))
        } else {
            EmptyIntArrayList
        }

        val allElementNameInfos = ArrayList<ElementNameInfo>()
        val visited = HashMap<String, ArrayList<SerialDescriptor>>() // key is SerialName
        visited[serialName] = arrayListOf(serialDescriptorUnsafe)

        totalElementCount = scanElements(
            useAlternativeNames, namingStrategy, serialDescriptorUnsafe,
            parentElementIndex = 0,
            elementOffsets = elementOffsets,
            allElementNameInfos = allElementNameInfos,
            visited = visited,
            level = 0,
            start = 0
        )

        registerNames(allElementNameInfos)

        if (classDiscriminator != null) {
            val element = getElementInfoByName(classDiscriminator)
            if (!element.isUnknown) {
                if (!element.isRegular) throw SerializationException(
                    "$serialName discriminator '$classDiscriminator' conflicts with inlined element"
                )
            }
        }

        if (hasJsonInlineElements) {
            this.elementOffsets = ShortArray(elementOffsets.size) { i -> elementOffsets[i].toShort() }
        }
    }

    fun getElementName(index: Int): String =
        elementNames?.get(index) ?: serialDescriptorUnsafe.getElementName(index)

    fun isElementJsonInline(elementIndex: Int): Boolean =
        getChildElementsOffset(elementIndex) != 0

    fun getChildElementsOffset(elementAbsoluteIndex: Int): Int {
        return (elementOffsets ?: return 0)[elementAbsoluteIndex].asInt()
    }

    private data class ElementNameInfo(
        val names: List<String>,
        val serialDescriptor: SerialDescriptor,
        val elementInfo: ElementInfo
    ) {
        val defaultName: String get() = names.last()
    }

    private fun scanElements(
        useAlternativeNames: Boolean,
        namingStrategy: JsonNamingStrategy?,
        serialDescriptor: SerialDescriptor,
        parentElementIndex: Int,
        elementOffsets: MutableIntList,
        allElementNameInfos: ArrayList<ElementNameInfo>,
        visited: HashMap<String, ArrayList<SerialDescriptor>>, // key is SerialName
        level: Int,
        start: Int
    ): Int {
        if (level > ElementInfo.MAX_LEVEL) throw SerializationException(
            "maximum nesting level of @JsonInline elements is ${ElementInfo.MAX_LEVEL}"
        )

        var offset = start

        if (level > 0 && serialDescriptor.kind is PolymorphicKind) {
            // scan all elements of polymorphic tree
            throw SerializationException("polymorphic classes can not be marked with @JsonInline")
        }

        offset += serialDescriptor.elementsCount

        if (serialDescriptor.kind.isClassLike()) {
            repeat(serialDescriptor.elementsCount) {
                elementOffsets.add(0)
            }
        }

        for (index in 0..<serialDescriptor.elementsCount) {
            val elementInfo = ElementInfo(
                index = start + index,
                inlineSiteIndex = if (level == 0) -1 else parentElementIndex
            )

            if (!serialDescriptor.hasElementAnnotation<JsonInline>(index)) {
                allElementNameInfos.add(
                    ElementNameInfo(
                        scanElementNames(useAlternativeNames, namingStrategy, serialDescriptor, index),
                        serialDescriptor,
                        elementInfo
                    )
                )
                continue
            }

            flags += FLAG_HAS_INLINE_ELEMENTS

            val elementDescriptor = serialDescriptor.getElementDescriptor(index)

            registerInVisited(visited, elementDescriptor)

            if (elementDescriptor.kind == StructureKind.MAP) {
                if (hasInlineMapElement) throw SerializationException(
                    "${serialDescriptor.serialName}: only one map-like element can be marked with @JsonInline"
                )

                inlineMapElement = ElementInfo(
                    index = offset++,
                    inlineSiteIndex = elementInfo.index
                )
            } else if (elementDescriptor.kind != StructureKind.CLASS) {
                throw SerializationException(
                    "${serialDescriptor.serialName}.${elementDescriptor.serialName}: " +
                    "only properties of regular class type can be marked with @JsonInline"
                )
            }

            assert { elementOffsets[elementInfo.index] == 0 }
            // ref to nested elements which will be scanned next
            elementOffsets[elementInfo.index] = offset

            if (elementDescriptor.kind != StructureKind.MAP) {
                offset = scanElements(
                    useAlternativeNames,
                    namingStrategy,
                    elementDescriptor,
                    parentElementIndex = elementInfo.index,
                    elementOffsets,
                    allElementNameInfos,
                    visited,
                    level = level + 1,
                    start = offset
                )
            }
        }

        return offset
    }

    private fun registerInVisited(
        visitedByName: HashMap<String, ArrayList<SerialDescriptor>>,
        elementDescriptor: SerialDescriptor
    ) {
        val visited = visitedByName.getOrPut(elementDescriptor.serialName) { ArrayList(2) }
        for (visitedDesc in visited) {
            if (visitedDesc == elementDescriptor || visitedDesc.nullable == elementDescriptor.nullable) {
                throw SerializationException("cyclic @JsonInline element '${elementDescriptor.serialName}'")
            }
        }
        visited.add(elementDescriptor)
    }

    private fun scanElementNames(
        useAlternativeNames: Boolean,
        namingStrategy: JsonNamingStrategy?,
        serialDescriptor: SerialDescriptor,
        elementIndex: Int
    ): List<String> {
        val jsonNames = when {
            !useAlternativeNames -> emptyArray<String>()
            else -> serialDescriptor.findElementAnnotation<JsonNames>(elementIndex)?.names.orEmpty()
        }

        return buildList(jsonNames.size + 1) {
            addAll(jsonNames)
            add(serialDescriptor.getElementName(elementIndex).let { serialName ->
                namingStrategy
                    ?.serialNameForJson(serialDescriptor, elementIndex, serialName)
                    ?: serialName
            })
        }
    }

    private fun registerNames(allElementNameInfos: ArrayList<ElementNameInfo>) {
        if (allElementNameInfos.all { it.names.all(StringsASCII::isAscii) }) {
            flags += FLAG_NAMES_ARE_ASCII
        }

        for (nameInfo in allElementNameInfos) {
            for (elementName in nameInfo.names) {
                putElementByKey(elementName, nameInfo.elementInfo)
            }
        }

        for (nameInfo in allElementNameInfos) {
            if (!nameInfo.elementInfo.isRegular) break
            putElementName(nameInfo.defaultName, nameInfo.elementInfo.index)
        }
    }

    private fun putElementByKey(name: String, elementInfo: ElementInfo) {
        val key = if (caseInsensitiveEnum) name.lowercase() else name

        if (elementByName.put(key, elementInfo.asInt, ElementInfo.UNKNOWN_NAME.asInt) != ElementInfo.UNKNOWN_NAME.asInt) {
            duplicateElementName(name)
        }

        if (caseInsensitiveEnum && allNamesAreAscii) {
            elementByName.put(key.encodeToByteArray().asBuffer(), elementInfo.asInt)
        } else {
            elementByName.put(key.asUtf8SubString(), elementInfo.asInt)
        }
    }

    private fun putElementName(name: String, elementIndex: Int) {
        if (name == serialDescriptorUnsafe.getElementName(elementIndex)) return

        var names: Array<String>? = elementNames
        if (names == null) {
            names = Array(serialDescriptorUnsafe.elementsCount) { index ->
                serialDescriptorUnsafe.getElementName(index)
            }
            elementNames = names
        }

        names[elementIndex] = name
    }

    private fun duplicateElementName(name: String): Nothing {
        throw SerializationException("Element with name '$name' appeared twice in class with serial name '$serialName'")
    }

    /**
     * WARN! For a nullable field returns a nullable descriptor,
     * However, [kotlinx.serialization.encoding.Decoder.decodeSerializableValue]
     * can be called with non-nullable descriptor.
     * This discrepancy in nullability does not affect anything,
     * because the parent descriptor will not be checked anywhere on nullability.
     */
    fun getElementDescriptor(index: Int, cache: DescriptorCache, elementDescriptor: SerialDescriptor): ZeroJsonDescriptor =
        elements.getOrNull(index) ?: cache.getOrCreate(elementDescriptor)

    fun getElementDescriptor(index: Int): ZeroJsonDescriptor? =
        elements.getOrNull(index)

    private fun getElementByCaseInsensitiveAsciiName(name: RandomAccessTextReaderSubString, tempBuffer: ArrayBuffer): ElementInfo {
        val nameLength = name.sourceLength
        var hash = 1
        name.reader.readAtPosition(name.start) {
            repeat(nameLength) { i ->
                val code = name.reader.readAsciiCode()
                if (code shr 7 != 0) return ElementInfo.UNKNOWN_NAME
                val lc = StringsASCII.lowercase(code)
                tempBuffer[i] = lc
                hash = hash * 31 + lc
            }
        }
        val oldSize = tempBuffer.size
        tempBuffer.setSize(nameLength)
        @OptIn(InternalBuffersApi::class)
        tempBuffer.setHashCodeUnsafe(hash)

        return getElementByName(tempBuffer).also {
            tempBuffer.setSize(oldSize)
            tempBuffer.resetHashCode()
        }
    }

    fun getElementInfoByName(name: AbstractSubString, tempBuffer: ArrayBuffer): ElementInfo =
        if (caseInsensitiveEnum) {
            if (allNamesAreAscii && name is RandomAccessTextReaderSubString) {
                getElementByCaseInsensitiveAsciiName(name, tempBuffer)
            } else {
                // slow path: may allocate 2 strings at worst
                getElementByName(name.toString().lowercase())
            }
        } else {
            getElementByName(name)
        }

    fun getElementInfoByName(name: String): ElementInfo =
        getElementByName(if (caseInsensitiveEnum) name.lowercase() else name)

    private fun getElementByName(name: Any): ElementInfo {
        val raw = elementByName.getOrDefault(name, ElementInfo.UNKNOWN_NAME.asInt)
        return when {
            hasInlineMapElement && raw == ElementInfo.UNKNOWN_NAME.asInt -> inlineMapElement
            else -> ElementInfo(raw)
        }
    }

    override fun toString(): String = buildString {
        appendLine("ZeroJsonDescriptor(")
        appendIndented("serialName = ")
        appendLine(serialName)
        appendIndented("kind = ")
        appendLine(kind)
        appendIndented("elementsCount = ")
        appendLine(elements.size)
        appendIndented("totalElementCount = ")
        appendLine(totalElementCount)
        if (classDiscriminator != null) {
            appendIndented("classDiscriminator = ")
            appendLine(classDiscriminator)
        }
        appendIndented("caseInsensitive = ")
        appendLine(caseInsensitiveEnum)
        if (caseInsensitiveEnum && allNamesAreAscii) {
            appendIndented("allNamesAreAscii = ")
            appendLine(allNamesAreAscii)
        }
        if (hasJsonInlineElements) {
            appendIndented("hasJsonInlineElements = ")
            appendLine(hasJsonInlineElements)
            if (inlineMapElement.asInt >= 0) {
                appendIndented("inlineMapElement = ")
                appendLine(inlineMapElement)
            }
        }
        appendLine(")")
    }

    companion object {
        @JvmName("create")
        @JvmStatic
        operator fun invoke(
            serialDescriptor: SerialDescriptor,
            config: DescriptorCacheConfig
        ): ZeroJsonDescriptor {
            val classDiscriminator = if (serialDescriptor.kind !is PolymorphicKind) null else {
                serialDescriptor.annotations
                    .findFirstInstanceOfOrNull<JsonClassDiscriminator>()
                    ?.discriminator ?: config.classDiscriminator
            }

            val descriptor = ZeroJsonDescriptor(
                serialDescriptor,
                elements = emptyArray<ZeroJsonDescriptor?>(),
                elementNames = null,
                elementByName = EmptyObject2IntHashMap,
                flags = Bits32(),
                inlineMapElement = ElementInfo.UNKNOWN_NAME,
                totalElementCount = -1,
                elementOffsets = null,
                kindFlags = serialDescriptor.kind.getFlags(),
                classDiscriminator = classDiscriminator,
                classDiscriminatorUtf8Encoded = classDiscriminator?.asUtf8SubString(),
                classDiscriminatorSubString = classDiscriminator?.substringWrapper()
            )

            val kind = serialDescriptor.kind
            if (kind == SerialKind.ENUM && config.decodeEnumsCaseInsensitive) {
                descriptor.flags += FLAG_CASE_INSENSITIVE
            }

            if (config.ignoreUnknownKeys ||
                serialDescriptor.annotations.findFirstInstanceOfOrNull<JsonIgnoreUnknownKeys>() != null) {
                descriptor.flags += FLAG_IGNORE_UNKNOWN_KEYS
            }

            return descriptor
        }

        private fun unsafe(serialDescriptor: SerialDescriptor, elements: Int = 0): ZeroJsonDescriptor =
            ZeroJsonDescriptor(serialDescriptor, ZeroJsonConfiguration.Default.descriptorCacheConfig).also {
                it.totalElementCount = elements
            }

        private val EmptyObject2IntHashMap = MutableObjectIntMap<Any>(0)
        private val EmptyIntArrayList = MutableIntList(0)

        @OptIn(InternalSerializationApi::class)
        @JvmField
        val POLYMORPHIC_VALUE_WRAPPER = unsafe(
            buildClassSerialDescriptor("[POLYMORPHIC_VALUE_WRAPPER]") {
                element<String>("type")
                element("value", buildSerialDescriptor("[POLYMORPHIC_WRAPPER_VALUE]", SerialKind.CONTEXTUAL))
            },
            elements = 2
        )

        @JvmField
        val MAP = unsafe(MapSerializer(NothingSerializer(), NothingSerializer()).descriptor)

        @JvmField
        val LIST = unsafe(ListSerializer(NothingSerializer()).descriptor)

        @JvmField
        val ROOT = unsafe(
            @OptIn(InternalSerializationApi::class)
            buildSerialDescriptor("<ROOT>", StructureKind.LIST, Unit.serializer().descriptor) {
                element<Unit>("element")
            }
        )

        @JvmField
        val NOP = ZeroJsonDescriptor(NothingSerializer().descriptor, ZeroJsonConfiguration.Default.descriptorCacheConfig)

        private const val FLAG_CASE_INSENSITIVE = 0
        private const val FLAG_NAMES_ARE_ASCII = 1
        private const val FLAG_HAS_INLINE_ELEMENTS = 2
        private const val FLAG_IGNORE_UNKNOWN_KEYS = 3
        private const val FLAG_MATERIALIZED_DISCRIMINATOR = 4
    }
}

internal val ZeroJsonDescriptor.isNullable: Boolean get() = serialDescriptorUnsafe.isNullable
internal val ZeroJsonDescriptor.serialName: String  get() = serialDescriptorUnsafe.serialName
internal val ZeroJsonDescriptor.elementsCount: Int  get() = serialDescriptorUnsafe.elementsCount
internal val ZeroJsonDescriptor.kind: SerialKind    get() = serialDescriptorUnsafe.kind

internal fun ZeroJsonDescriptor.needWrappingIfSubclass(): Boolean =
    isNullable || !kind.let { it == StructureKind.CLASS || it == StructureKind.OBJECT }

internal fun ZeroJsonDescriptor.discriminatorSubStringFor(textReader: RandomAccessTextReader): AbstractSubString? =
    if (textReader is Utf8TextReader) {
        classDiscriminatorUtf8Encoded
    } else {
        classDiscriminatorSubString
    }