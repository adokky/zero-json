package kotlinx.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EncodingExtensionsTest {

    @Serializable(with = BoxSerializer::class)
    class Box(val i: Int)

    object BoxSerializer : KSerializer<Box> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Box") {
            element<Int>("i")
        }

        override fun serialize(encoder: Encoder, value: Box) {
            encoder.encodeStructure(descriptor) {
                throw ArithmeticException()
            }
        }

        override fun deserialize(decoder: Decoder): Box {
            decoder.decodeStructure(descriptor) {
                throw ArithmeticException()
            }
        }
    }

    @Test
    fun testEncodingExceptionNotSwallowed() {
        assertFailsWith<ArithmeticException> { Json.encodeToString(Box(1)) }
    }

    @Test
    fun testDecodingExceptionNotSwallowed() {
        assertFailsWith<ArithmeticException> { Json.decodeFromString<Box>("""{"i":1}""") }
    }
}
