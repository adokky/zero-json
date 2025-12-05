package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.Id
import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals

class PolymorphismTestKtx : JsonTestBase() {

    @Serializable
    private data class Wrapper(
        @Id(1) @Polymorphic val polyBase1: PolyBase,
        @Id(2) @Polymorphic val polyBase2: PolyBase
    )

    private val module: SerializersModule = BaseAndDerivedModule + SerializersModule {
        polymorphic(PolyDerived::class, PolyDerived.serializer())
    }

    private object EvenDefaultSerializer : KSerializer<PolyBase> {
        override val descriptor = buildClassSerialDescriptor("even") {
            element<String>("parity")
        }

        override fun serialize(encoder: Encoder, value: PolyBase) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "even")
            }
        }

        override fun deserialize(decoder: Decoder): PolyBase = decoder.decodeStructure(descriptor) {
            decodeElementIndex(descriptor)
            assertEquals("even", decodeStringElement(descriptor, 0))
            PolyDefaultWithId(0)
        }
    }

    private object OddDefaultSerializer : KSerializer<PolyBase> {
        override val descriptor = buildClassSerialDescriptor("odd") {
            element<String>("parity")
        }

        override fun serialize(encoder: Encoder, value: PolyBase) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "odd")
            }
        }

        override fun deserialize(decoder: Decoder): PolyBase = decoder.decodeStructure(descriptor) {
            decodeElementIndex(descriptor)
            assertEquals("odd", decodeStringElement(descriptor, 0))
            PolyDefaultWithId(1)
        }
    }

    @Test
    fun testDefaultSerializer() = parametrizedTest {
        val json = ZeroJson {
            serializersModule = module + SerializersModule {
                polymorphicDefaultSerializer(PolyBase::class) { value ->
                    if (value.id % 2 == 0) EvenDefaultSerializer else OddDefaultSerializer
                }
                polymorphicDefaultDeserializer(PolyBase::class) { disc ->
                    if (disc == EvenDefaultSerializer.descriptor.serialName) EvenDefaultSerializer else OddDefaultSerializer
                }
            }
        }
        val obj = Wrapper(PolyDefaultWithId(0), PolyDefaultWithId(1))
        val str = """{"polyBase1":{"type":"even","parity":"even"},"polyBase2":{"type":"odd","parity":"odd"}}"""
        assertEquals(obj, json.decodeFromString<Wrapper>(str))
        assertEquals(str, json.encodeToString(Wrapper.serializer(), obj))
    }

    @Serializable
    private sealed class Conf {
        @Serializable
        @SerialName("empty")
        data object Empty : Conf() // default

        @Serializable
        @SerialName("simple")
        data class Simple(val value: String) : Conf()
    }

    private val jsonForConf = ZeroJson {
        isLenient = false
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphicDefaultDeserializer(Conf::class) { Conf.Empty.serializer() }
        }
    }

    @Test
    fun defaultSerializerWithEmptyBodyTest() = parametrizedTest { 
        assertEquals(Conf.Simple("123"), jsonForConf.decodeFromString<Conf>("""{"type": "simple", "value": "123"}"""))
        assertEquals(Conf.Empty, jsonForConf.decodeFromString<Conf>("""{"type": "default"}"""))
        assertEquals(Conf.Empty, jsonForConf.decodeFromString<Conf>("""{"unknown": "Meow"}"""))
        assertEquals(Conf.Empty, jsonForConf.decodeFromString<Conf>("""{}"""))
    }

    @Test
    fun testTypeKeysInLenientMode() = parametrizedTest { 
        val json = ZeroJson(jsonForConf) { isLenient = true }

        assertEquals(Conf.Simple("123"), json.decodeFromString<Conf>("""{type: simple, value: 123}"""))
        assertEquals(Conf.Empty, json.decodeFromString<Conf>("""{type: default}"""))
        assertEquals(Conf.Empty, json.decodeFromString<Conf>("""{unknown: Meow}"""))
        assertEquals(Conf.Empty, json.decodeFromString<Conf>("""{}"""))
    }
}
