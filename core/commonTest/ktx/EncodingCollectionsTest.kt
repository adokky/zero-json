package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
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
        assertEquals("""["Hello","World!"]""", TestZeroJson.encodeToString(ListSerializer, listOf("Hello", "World!")))
    }
}
