@file:OptIn(ExperimentalStdlibApi::class)

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContextAndPolymorphicTest {
    @Serializable
    private data class Data(val a: Int, val b: Int = 42)

    @Serializable
    private data class EnhancedData(
        val data: Data,
        @Contextual val stringPayload: Payload,
        @Serializable(with = BinaryPayloadSerializer::class) val binaryPayload: Payload
    )

    @Serializable
    @SerialName("Payload")
    private data class Payload(val s: String)

    @Serializable
    private data class PayloadList(val ps: List<@Contextual Payload>)

    private object BinaryPayloadSerializer : KSerializer<Payload> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BinaryPayload", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Payload) {
            encoder.encodeString(value.s.encodeToByteArray().toHexString())
        }

        override fun deserialize(decoder: Decoder): Payload {
            return Payload(decoder.decodeString().hexToByteArray().decodeToString())
        }
    }

    private val value = EnhancedData(Data(100500), Payload("string"), Payload("binary"))
    private lateinit var json: ZeroJson

    @BeforeTest
    fun initContext() {
        val scope = SerializersModule {  }
        val bPolymorphicModule = SerializersModule { polymorphic(Any::class) { subclass(serializer<Payload>()) } }
        json = ZeroJson {
            encodeDefaults = true
            serializersModule = scope + bPolymorphicModule
        }
    }

    @Test
    fun testWriteCustom() {
        val s = json.encodeToString(EnhancedData.serializer(), value)
        assertEquals("""{"data":{"a":100500,"b":42},"stringPayload":{"s":"string"},"binaryPayload":"62696e617279"}""", s)
    }

    @Test
    fun testReadCustom() {
        val s = json.decodeFromString(
            EnhancedData.serializer(),
            """{"data":{"a":100500,"b":42},"stringPayload":{"s":"string"},"binaryPayload":"62696e617279"}""")
        assertEquals(value, s)
    }

    @Test
    fun testWriteCustomList() {
        val s = json.encodeToString(PayloadList.serializer(), PayloadList(listOf(Payload("1"), Payload("2"))))
        assertEquals("""{"ps":[{"s":"1"},{"s":"2"}]}""", s)
    }

    @Test
    fun testPolymorphicResolve() {
        val map = mapOf<String, Any>("Payload" to Payload("data"))
        val serializer = MapSerializer(String.serializer(), PolymorphicSerializer(Any::class))
        val s = json.encodeToString(serializer, map)
        assertEquals("""{"Payload":{"type":"Payload","s":"data"}}""", s)
    }

    @Test
    fun testDifferentRepresentations() {
        val simpleModule = SerializersModule {  }
        val binaryModule = serializersModuleOf(BinaryPayloadSerializer)

        val json1 = ZeroJson { serializersModule = simpleModule }
        val json2 = ZeroJson { serializersModule = binaryModule }

        // in json1, Payload would be serialized with PayloadSerializer,
        // in json2, Payload would be serialized with BinaryPayloadSerializer

        val list = PayloadList(listOf(Payload("string")))
        assertEquals("""{"ps":[{"s":"string"}]}""", json1.encodeToString(PayloadList.serializer(), list))
        assertEquals("""{"ps":["737472696e67"]}""", json2.encodeToString(PayloadList.serializer(), list))
    }

    private fun SerialDescriptor.inContext(module: SerializersModule): SerialDescriptor = when (kind) {
        SerialKind.CONTEXTUAL -> requireNotNull(module.getContextualDescriptor(this)) { "Expected $this to be registered in module" }
        else -> error("Expected this function to be called on CONTEXTUAL descriptor")
    }

    @Test
    fun testResolveContextualDescriptor() {
        val binaryModule = serializersModuleOf(BinaryPayloadSerializer)

        val contextDesc = EnhancedData.serializer().descriptor.elementDescriptors.toList()[1] // @ContextualSer stringPayload
        assertEquals(SerialKind.CONTEXTUAL, contextDesc.kind)
        assertEquals(0, contextDesc.elementsCount)

        val resolvedToBinary = contextDesc.inContext(binaryModule)
        assertEquals(PrimitiveKind.STRING, resolvedToBinary.kind)
        assertEquals("BinaryPayload", resolvedToBinary.serialName)
    }

    @Test
    fun testContextualSerializerUsesDefaultIfModuleIsEmpty() {
        val jsonArrayWithDefaults = ZeroJson { encodeDefaults = true }
        val s = jsonArrayWithDefaults.encodeToString(EnhancedData.serializer(), value)
        assertEquals("""{"data":{"a":100500,"b":42},"stringPayload":{"s":"string"},"binaryPayload":"62696e617279"}""", s)
    }
}
