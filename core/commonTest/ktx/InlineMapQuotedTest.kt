package dev.dokky.zerojson.ktx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import kotlin.jvm.JvmInline
import kotlin.test.Test

class InlineMapQuotedTest : JsonTestBase() {
    @Serializable(with = CustomULong.Serializer::class)
    private data class CustomULong(val value: ULong) {
        @OptIn(ExperimentalUnsignedTypes::class)
        object Serializer : KSerializer<CustomULong> {
            override val descriptor: SerialDescriptor =
                @OptIn(ExperimentalUnsignedTypes::class) ULong.serializer().descriptor
            override fun deserialize(decoder: Decoder): CustomULong =
                CustomULong(decoder.decodeInline(descriptor).decodeSerializableValue(ULong.serializer()))
            override fun serialize(encoder: Encoder, value: CustomULong) {
                encoder.encodeInline(descriptor).encodeSerializableValue(ULong.serializer(), value.value)
            }
        }
    }

    @JvmInline
    @Serializable
    private value class WrappedLong(val value: Long)

    @JvmInline
    @Serializable
    private value class WrappedULong(val value: ULong)

    @Serializable
    private data class Carrier(
        val mapLong: Map<Long, Long>,
        val mapULong: Map<ULong, Long>,
        val wrappedLong: Map<WrappedLong, Long>,
        val mapWrappedU: Map<WrappedULong, Long>,
        val mapCustom: Map<CustomULong, Long>
    )

    @Test
    fun testInlineClassAsMapKey() {
        assertJsonFormAndRestored(
            serializer<Carrier>(),
            Carrier(
                mapOf(1L to 1L),
                mapOf(Long.MAX_VALUE.toULong() + 2UL to 2L),
                mapOf(WrappedLong(3L) to 3L),
                mapOf(WrappedULong(Long.MAX_VALUE.toULong() + 4UL) to 4L),
                mapOf(CustomULong(Long.MAX_VALUE.toULong() + 5UL) to 5L)
            ),
            """{"mapLong":{"1":1},"mapULong":{"9223372036854775809":2},"wrappedLong":{"3":3},"mapWrappedU":{"9223372036854775811":4},"mapCustom":{"9223372036854775812":5}}"""
        )
    }
}
