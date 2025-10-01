@file:UseContextualSerialization(ContextualTest.Cont::class)

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals

class ContextualTest {
    data class Cont(val i: Int)

    @Serializable
    private data class DateHolder(val cont: Cont?)

    private object DateSerializer: KSerializer<Cont> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Cont = Cont(decoder.decodeInt())
        override fun serialize(encoder: Encoder, value: Cont) { encoder.encodeInt(value.i) }
    }

    @Test
    fun test() {
        val json = ZeroJson {
            serializersModule = SerializersModule { contextual(DateSerializer) }
            explicitNulls = true
        }
        assertEquals("""{"cont":42}""", json.encodeToString(DateHolder(Cont(42))))
        assertEquals("""{"cont":null}""", json.encodeToString(DateHolder(null)))
    }
}
