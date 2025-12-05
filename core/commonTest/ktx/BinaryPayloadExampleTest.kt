package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BinaryPayloadExampleTest {
    @Serializable(BinaryPayload.Companion::class)
    class BinaryPayload(val req: ByteArray, val res: ByteArray) {
        @OptIn(ExperimentalStdlibApi::class)
        companion object : KSerializer<BinaryPayload> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BinaryPayload") {
                element("req", ByteArraySerializer().descriptor)
                element("res", ByteArraySerializer().descriptor)
            }

            override fun serialize(encoder: Encoder, value: BinaryPayload) {
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, value.req.toHexString())
                    encodeStringElement(descriptor, 1, value.res.toHexString())
                }
            }

            override fun deserialize(decoder: Decoder): BinaryPayload {
                return decoder.decodeStructure(descriptor) {
                    var req: ByteArray? = null // consider using flags or bit mask if you
                    var res: ByteArray? = null // need to read nullable non-optional properties
                    loop@ while (true) {
                        when (val i = decodeElementIndex(descriptor)) {
                            CompositeDecoder.DECODE_DONE -> break@loop
                            0 -> req = decodeStringElement(descriptor, i).hexToByteArray()
                            1 -> res = decodeStringElement(descriptor, i).hexToByteArray()
                            else -> throw SerializationException("Unknown index $i")
                        }
                    }
                    BinaryPayload(
                        req ?: throw SerializationException("MFE: req"),
                        res ?: throw SerializationException("MFE: res")
                    )
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as BinaryPayload

            if (!req.contentEquals(other.req)) return false
            if (!res.contentEquals(other.res)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = req.contentHashCode()
            result = 31 * result + res.contentHashCode()
            return result
        }
    }

    @Test
    fun payloadEquivalence() {
        val payload1 = BinaryPayload(byteArrayOf(0, 0, 0), byteArrayOf(127, 127))
        val s = TestZeroJson.encodeToString(BinaryPayload.serializer(), payload1)
        assertEquals("""{"req":"000000","res":"7f7f"}""", s)
        val payload2 = TestZeroJson.decodeFromString(BinaryPayload.serializer(), s)
        assertEquals(payload1, payload2)
    }
}