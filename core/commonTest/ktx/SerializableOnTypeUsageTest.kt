@file:Suppress("EXTERNAL_SERIALIZER_USELESS")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializableOnTypeUsageTest {
    @Test
    fun testAnnotationIsApplied() {
        val data = SerializableOnArguments(listOf(1, 2), listOf(listOf(IntHolder(42))))
        val str = ZeroJson.encodeToString(SerializableOnArguments.serializer(), data)
        assertEquals("""{"list1":[2,4],"list2":[[84]]}""", str)
        val restored = ZeroJson.decodeFromString(SerializableOnArguments.serializer(), str)
        assertEquals(data, restored)
    }

    @Test
    fun testOnProperties() {
        val str = ZeroJson.encodeToString(Carrier.serializer(), Carrier(IntHolder(42), 2))
        assertEquals("""{"a":84,"i":4}""", str)
    }

    private object MultiplyingIntSerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("MultiplyingInt", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): Int = decoder.decodeInt() / 2
        override fun serialize(encoder: Encoder, value: Int) { encoder.encodeInt(value * 2) }
    }

    @Serializable(with = DividingIntHolderSerializer::class)
    private data class IntHolder(val data: Int)

    @Serializer(IntHolder::class)
    private object MultiplyingIntHolderSerializer {
        override val descriptor = buildSerialDescriptor("IntHolder", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder) = IntHolder(decoder.decodeInt() / 2)
        override fun serialize(encoder: Encoder, value: IntHolder) {
            encoder.encodeInt(value.data * 2)
        }
    }

    @Serializer(IntHolder::class)
    private object DividingIntHolderSerializer {
        override val descriptor = buildSerialDescriptor("IntHolder", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): IntHolder = IntHolder(decoder.decodeInt() * 2)
        override fun serialize(encoder: Encoder, value: IntHolder) {
            encoder.encodeInt(value.data / 2)
        }
    }

    @Serializable
    private data class Carrier(
        @Serializable(with = MultiplyingIntHolderSerializer::class) val a: IntHolder,
        @Serializable(with = MultiplyingIntSerializer::class) val i: Int
    )

    @Serializable
    private data class SerializableOnArguments(
        val list1: List<@Serializable(MultiplyingIntSerializer::class) Int>,
        val list2: List<List<@Serializable(MultiplyingIntHolderSerializer::class) IntHolder>>
    )
}