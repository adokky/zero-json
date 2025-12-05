package dev.dokky.zerojson

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

class PolymorphismSpecialCases {
    @Serializable
    private sealed interface Base {
        @Serializable
        @JvmInline
        value class Ctx(@Contextual val ctx: SimpleDataClass): Base

        @Serializable
        @JvmInline
        value class Primitive(val int: Int): Base

        object SimpleDataClassAsString: KSerializer<SimpleDataClass> {
            override val descriptor: SerialDescriptor
                get() = String.serializer().descriptor

            override fun serialize(encoder: Encoder, value: SimpleDataClass) {
                encoder.encodeString(value.key)
            }

            override fun deserialize(decoder: Decoder): SimpleDataClass =
                SimpleDataClass(decoder.decodeString())
        }
    }

    @Serializable
    private data class PolyHolder(val base: Base)

    @Test
    fun contextual_sub_class_with_custom_serializer() {
        val json = ZeroJson {
            serializersModule = SerializersModule {
                contextual(SimpleDataClass::class, Base.SimpleDataClassAsString)
            }
        }

        val value = Base.Ctx(SimpleDataClass("zzz"))
        val encoded = json.encodeToString<Base>(value)
        assertEquals(value, json.decodeFromString<Base>(encoded))
    }

    @Test
    fun primitive_sub_class() {
        val value = Base.Primitive(123)
        val encoded = TestZeroJson.encodeToString<Base>(value)
        assertEquals(value, TestZeroJson.decodeFromString<Base>(encoded))
    }

    @Test
    fun primitive_sub_class_inside_holder() {
        val value = PolyHolder(Base.Primitive(123))
        val encoded = TestZeroJson.encodeToString<PolyHolder>(value)
        assertEquals(value, TestZeroJson.decodeFromString<PolyHolder>(encoded))
    }
}