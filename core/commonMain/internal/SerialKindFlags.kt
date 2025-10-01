package dev.dokky.zerojson.internal

import androidx.collection.MutableObjectIntMap
import karamel.utils.AutoBitDescriptors
import karamel.utils.Bits32
import karamel.utils.toBoolean
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

private object SerialKindBits: AutoBitDescriptors() {
    val isClassLike = uniqueBit()
    val isMap = uniqueBit()
    val commaAfterEachElement = uniqueBit()
    val isQuoted = uniqueBit()
    val isList = uniqueBit()

    val isJsonObject = isClassLike or isMap
    val isCompound = isJsonObject or isList
    val isCollection = isMap or isList
}

@JvmInline
internal value class SerialKindFlags private constructor(private val bits: Bits32<SerialKindBits>) {
    constructor(
        isClassLike: Boolean,
        isMap: Boolean,
        commaAfterEachElement: Boolean,
        isQuoted: Boolean,
        isList: Boolean
    ): this(
        (SerialKindBits.isClassLike           and isClassLike) +
        (SerialKindBits.isMap                 and isMap) +
        (SerialKindBits.commaAfterEachElement and commaAfterEachElement) +
        (SerialKindBits.isQuoted              and isQuoted) +
        (SerialKindBits.isList                and isList)
    )

    val commaAfterEachElement: Boolean get() = bits.containsAny(SerialKindBits.commaAfterEachElement)
    val isClassLike: Boolean           get() = bits.containsAny(SerialKindBits.isClassLike)
    val isMap: Boolean                 get() = bits.containsAny(SerialKindBits.isMap)
    /** `true` if element always quoted */
    val isQuoted: Boolean              get() = bits.containsAny(SerialKindBits.isQuoted)
    val isList: Boolean                get() = bits.containsAny(SerialKindBits.isList)
    val isJsonObject: Boolean          get() = bits.containsAny(SerialKindBits.isJsonObject)
    val isCompound: Boolean            get() = bits.containsAny(SerialKindBits.isCompound)
    val isCollection: Boolean          get() = bits.containsAny(SerialKindBits.isCollection)

    private fun rawFlag(bit: Bits32<SerialKindBits>): Int = (bits and bit).toInt()

    private fun rawIsMapKey(elementIndex: Int): Int =
        rawFlag(SerialKindBits.isMap) * (elementIndex.inv() and 1)
    fun isMapKey(elementIndex: Int): Boolean = rawIsMapKey(elementIndex).toBoolean()

    fun needCommaBeforeElement(elementIndex: Int): Boolean =
        (rawFlag(SerialKindBits.commaAfterEachElement) or rawIsMapKey(elementIndex)).toBoolean()

    companion object {
        @JvmStatic
        fun raw(flags: Int) = SerialKindFlags(Bits32(flags))

        @JvmStatic
        fun asInt(flags: SerialKindFlags): Int = flags.bits.toInt()
    }

    override fun toString(): String =
        "SerialKindFlags(" +
            "isMap=$isMap, " +
            "hasNamedElements=$isClassLike, " +
            "commaAfterEachElement=$commaAfterEachElement, " +
            "isQuoted=$isQuoted, " +
            "isJsonObject=$isJsonObject" +
            "isList=$isList" +
        ")"
}

private fun kindFlags(
    isClassLike: Boolean,
    isMap: Boolean = false,
    commaAfterEachElement: Boolean = true,
    isQuoted: Boolean = false,
    isList: Boolean = false,
): Int = SerialKindFlags.asInt(SerialKindFlags(
    isClassLike = isClassLike,
    isMap = isMap,
    commaAfterEachElement = commaAfterEachElement,
    isQuoted = isQuoted,
    isList = isList
))

private val flagsByKind = MutableObjectIntMap<SerialKind>(20).apply {
    put(PolymorphicKind.OPEN,   kindFlags(isClassLike = true))
    put(PolymorphicKind.SEALED, kindFlags(isClassLike = true))
    put(StructureKind.CLASS,    kindFlags(isClassLike = true))
    put(StructureKind.OBJECT,   kindFlags(isClassLike = true))
    put(StructureKind.LIST,     kindFlags(isClassLike = false, isList = true))
    put(StructureKind.MAP,      kindFlags(isClassLike = false, isMap = true, commaAfterEachElement = false))
    put(PrimitiveKind.STRING,   kindFlags(isClassLike = false, commaAfterEachElement = false, isQuoted = true))
    put(PrimitiveKind.CHAR,     kindFlags(isClassLike = false, commaAfterEachElement = false, isQuoted = true))
}

internal fun SerialKind.getFlags(): SerialKindFlags = SerialKindFlags.raw(flagsByKind.getOrDefault(this, 0))

internal fun SerialKind.isClassLike(): Boolean = getFlags().isClassLike

internal fun SerialKind.isCollection(): Boolean =
    this == StructureKind.MAP || this == StructureKind.LIST

internal fun SerialKind.openingBracket(): Char = if (this == StructureKind.LIST) '[' else '{'
internal fun SerialKind.closingBracket(): Char = if (this == StructureKind.LIST) ']' else '}'