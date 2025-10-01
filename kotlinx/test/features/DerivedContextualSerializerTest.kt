package kotlinx.serialization.features

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DerivedContextualSerializerTest {

    @Serializable
    abstract class Message

    @Serializable
    class SimpleMessage(val body: String) : Message()

    @Serializable
    class Holder(@Contextual val message: Message)

    object MessageAsStringSerializer : KSerializer<Message> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("kotlinx.serialization.MessageAsStringSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Message) {
            // dummy serializer that assumes Message is always SimpleMessage
            check(value is SimpleMessage)
            encoder.encodeString(value.body)
        }

        override fun deserialize(decoder: Decoder): Message {
            return SimpleMessage(decoder.decodeString())
        }
    }

    @Test
    fun testDerivedContextualSerializer() {
        val module = SerializersModule {
            contextual(MessageAsStringSerializer)
        }
        val format = Json { serializersModule = module }
        val data = Holder(SimpleMessage("hello"))
        val serialized = format.encodeToString(data)
        assertEquals("""{"message":"hello"}""", serialized)
        val deserialized = format.decodeFromString<Holder>(serialized)
        assertTrue(deserialized.message is SimpleMessage)
        assertEquals("hello", deserialized.message.body)
    }
}
