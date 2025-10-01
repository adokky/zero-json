package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJsonCompat
import dev.dokky.zerojson.ZeroJsonDecoder
import dev.dokky.zerojson.ZeroJsonEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals

class PolymorphismForCustomTest : JsonTestBase() {

    private val customSerializer = object : KSerializer<VImpl> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("VImpl") {
                element("a", String.serializer().descriptor)
            }

        override fun deserialize(decoder: Decoder): VImpl {
            decoder as ZeroJsonDecoder
            val jsonObject = decoder.decodeJsonElement() as JsonObject
            return VImpl(
                (jsonObject["a"] as JsonPrimitive).content
            )
        }

        override fun serialize(encoder: Encoder, value: VImpl) {
            encoder as ZeroJsonEncoder
            encoder.encodeJsonElement(
                JsonObject(mapOf("a" to JsonPrimitive(value.a)))
            )
        }
    }

    @Serializable
    private data class ValueHolder<V : Any>(
        @Polymorphic val value: V,
    )

    private data class VImpl(val a: String)

    @Test
    fun test() = parametrizedTest {
        val json = ZeroJsonCompat {
            serializersModule = SerializersModule {
                polymorphic(Any::class, VImpl::class, customSerializer)
            }
        }
        val valueHolder = ValueHolder(VImpl("aaa"))
        val encoded = json.encodeToString(ValueHolder.serializer(customSerializer), valueHolder)
        assertEquals("""{"value":{"type":"VImpl","a":"aaa"}}""", encoded)
        val decoded = json.decodeFromString<ValueHolder<*>>(ValueHolder.serializer(customSerializer), encoded)
        assertEquals(valueHolder, decoded)
    }
}
