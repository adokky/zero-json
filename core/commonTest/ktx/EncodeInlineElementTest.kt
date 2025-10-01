package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.framework.assertStringFormAndRestored
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.test.Test

@Serializable(WithUnsignedSerializer::class)
private data class WithUnsigned(val u: UInt)

private object WithUnsignedSerializer : KSerializer<WithUnsigned> {
    override fun serialize(encoder: Encoder, value: WithUnsigned) {
        encoder.encodeStructure(descriptor) {
            encodeInlineElement(descriptor, 0).encodeInt(value.u.toInt())
        }
    }

    override fun deserialize(decoder: Decoder): WithUnsigned = decoder.decodeStructure(descriptor) {
        var u: UInt = 0.toUInt()
        loop@ while (true) {
            u = when (val i = decodeElementIndex(descriptor)) {
                0 -> decodeInlineElement(descriptor, i).decodeInt().toUInt()
                else -> break@loop
            }
        }
        WithUnsigned(u)
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WithUnsigned") {
        element("u", UInt.serializer().descriptor)
    }
}

class EncodeInlineElementTest {
    @Test
    fun wrapper() {
        assertStringFormAndRestored<WithUnsigned>(
            """{"u":2147483648}""",
            WithUnsigned(Int.MAX_VALUE.toUInt() + 1.toUInt()),
            WithUnsignedSerializer
        )
    }
}
