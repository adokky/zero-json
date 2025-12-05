package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EncodingExtensionsTest {

    @Serializable(with = BoxSerializer::class)
    private class Box(val i: Int)

    private object BoxSerializer : KSerializer<Box> {
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
        assertFailsWith<ArithmeticException> { TestZeroJson.encodeToString(Box(1)) }
    }

    @Test
    fun testDecodingExceptionNotSwallowed() {
        assertFailsWith<ArithmeticException> { TestZeroJson.decodeFromString<Box>("""{"i":1}""") }
    }
}
