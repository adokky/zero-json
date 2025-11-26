package kotlinx.serialization

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class EncodingCollectionsTest {
    object ListSerializer : KSerializer<List<String>> {
        override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

        override fun serialize(encoder: Encoder, value: List<String>) {
            encoder.encodeCollection(descriptor, value) { index, item ->
                encodeStringElement(descriptor, index, item)
            }
        }

        override fun deserialize(decoder: Decoder): List<String> = throw NotImplementedError()
    }

    @Test
    fun testEncoding() {
        assertEquals("""["Hello","World!"]""", Json.encodeToString(ListSerializer, listOf("Hello", "World!")))
    }
}
