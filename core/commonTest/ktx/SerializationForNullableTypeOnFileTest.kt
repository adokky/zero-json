@file:UseSerializers(NullableIntSerializer::class, NonNullableIntSerializer::class)

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ktx.SerializationForNullableTypeOnFileTest.NonNullableIntSerializer
import dev.dokky.zerojson.ktx.SerializationForNullableTypeOnFileTest.NullableIntSerializer
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationForNullableTypeOnFileTest {

    @Serializable
    private data class Holder(val nullable: Int?, val nonNullable: Int)

    object NullableIntSerializer : KSerializer<Int?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableIntSerializer", PrimitiveKind.INT).nullable

        override fun serialize(encoder: Encoder, value: Int?) {
            if (value == null) encoder.encodeNull()
            else encoder.encodeInt(value + 1)
        }
        override fun deserialize(decoder: Decoder): Int? {
            return if (decoder.decodeNotNullMark()) {
                val value = decoder.decodeInt()
                value - 1
            } else {
                decoder.decodeNull()
            }
        }
    }

    object NonNullableIntSerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotNullIntSerializer", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Int) {
            return encoder.encodeInt(value + 2)
        }

        override fun deserialize(decoder: Decoder): Int {
            return (decoder.decodeInt() - 2)
        }
    }

    @Test
    fun testFileLevel() {
        assertEquals("""{"nullable":null,"nonNullable":52}""",
            ZeroJson.encodeToString(Holder(nullable = null, nonNullable = 50)))
        assertEquals("""{"nullable":1,"nonNullable":2}""",
            ZeroJson.encodeToString(Holder(nullable = 0, nonNullable = 0)))
        assertEquals("""{"nullable":11,"nonNullable":52}""",
            ZeroJson.encodeToString(Holder(nullable = 10, nonNullable = 50)))

        assertEquals(Holder(nullable = 0, nonNullable = 50),
            ZeroJson.decodeFromString("""{"nullable":1,"nonNullable":52}"""))
        assertEquals(Holder(nullable = null, nonNullable = 50),
            ZeroJson.decodeFromString("""{"nullable":null,"nonNullable":52}"""))
        assertEquals(Holder(nullable = 10, nonNullable = 50),
            ZeroJson.decodeFromString("""{"nullable":11,"nonNullable":52}"""))
    }
}
