package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import karamel.utils.assert
import kotlinx.serialization.encoding.CompositeDecoder
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

@JvmInline
internal value class ElementInfo(val asInt: Int) {
    constructor(index: Int, inlineSiteIndex: Int): this(
        (index or (inlineSiteIndex shl ZeroJsonConfiguration.ELEMENT_INDEX_NUM_BITS))
            .and(0.inv() ushr 1)
            .also {
                require(index in 0..ZeroJsonConfiguration.MAX_PROPERTY_ELEMENT_INDEX)
                require(inlineSiteIndex in -1..ZeroJsonConfiguration.MAX_PROPERTY_ELEMENT_INDEX)
            }
    )

    val index: Int get() = asInt and (ZeroJsonConfiguration.MAX_PROPERTY_ELEMENT_INDEX or (1 shl 31))
    /** Index of parental class @JsonInline-element, according to which lies the class of the current element */
    val inlineSiteIndex: Int get() = asInt.shl(1).shr(ZeroJsonConfiguration.ELEMENT_INDEX_NUM_BITS + 1)

    val isRegular: Boolean get() = asInt.ushr(30).xor(1) == 0
    val isJsonInlined: Boolean get() = asInt.ushr(30) == 0
    val isValid: Boolean get() = asInt >= 0
    val isUnknown: Boolean get() = asInt == CompositeDecoder.UNKNOWN_NAME

    override fun toString(): String {
        if (asInt < 0) return when(asInt) {
            CompositeDecoder.UNKNOWN_NAME -> "UNKNOWN_NAME"
            CompositeDecoder.DECODE_DONE -> "DECODE_DONE"
            else -> "INVALID($asInt)"
        }

        return "ElementInfo(index=$index, inlineSiteIndex=$inlineSiteIndex)"
    }

    companion object {
        @JvmStatic val UNKNOWN_NAME: ElementInfo = ElementInfo(CompositeDecoder.UNKNOWN_NAME) // -3

        @JvmStatic val MAX_LEVEL: Int = (0xff shr 2)

        @JvmStatic private val SPECIAL_SIGNIFICANT_BITS: Int = 2 /* sign(1) + isNotInlined(1) */

        init {
            assert {
                ZeroJsonConfiguration.ELEMENT_INDEX_NUM_BITS * 2 + SPECIAL_SIGNIFICANT_BITS <= 32
            }
        }
    }
}