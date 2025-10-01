@file:UseSerializers(GenericSerializersOnFileTest.MySerializer::class)

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class GenericSerializersOnFileTest {
    data class GenericClass<T>(val t: T)

    @Serializable
    private data class Holder(val notnull: GenericClass<String>, val nullable: GenericClass<String>?)

    class MySerializer<E>(val tSer: KSerializer<E>) : KSerializer<GenericClass<E>> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("my int descriptor", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: GenericClass<E>) {
            encoder.encodeString(value.t as String)
        }

        @Suppress("UNCHECKED_CAST")
        override fun deserialize(decoder: Decoder): GenericClass<E> {
            return GenericClass(decoder.decodeString() as E)
        }
    }

    @Test
    fun testSerialize() {
        assertEquals(
            """{"notnull":"Not Null","nullable":null}""",
            ZeroJson.KtxCompat.encodeToString(Holder(notnull = GenericClass("Not Null"), nullable = null))
        )
        assertEquals(
            """{"notnull":"Not Null","nullable":"Nullable"}""",
            ZeroJson.KtxCompat.encodeToString(
                Holder(
                    notnull = GenericClass("Not Null"),
                    nullable = GenericClass("Nullable")
                )
            )
        )
    }

    @Test
    fun testDeserialize() {
        assertEquals(
            Holder(notnull = GenericClass("Not Null"), nullable = null),
            ZeroJson.KtxCompat.decodeFromString("""{"notnull":"Not Null","nullable":null}""")
        )
        assertEquals(
            Holder(notnull = GenericClass("Not Null"), nullable = GenericClass("Nullable")),
            ZeroJson.KtxCompat.decodeFromString("""{"notnull":"Not Null","nullable":"Nullable"}""")
        )
    }
}
