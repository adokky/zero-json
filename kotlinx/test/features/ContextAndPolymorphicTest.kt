/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.test.InternalHexConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class ContextAndPolymorphicTest {

    @Serializable
    data class Data(val a: Int, val b: Int = 42)

    @Serializable
    data class EnhancedData(
        val data: Data,
        @Contextual val stringPayload: Payload,
        @Serializable(with = BinaryPayloadSerializer::class) val binaryPayload: Payload
    )

    @Serializable
    @SerialName("Payload")
    data class Payload(val s: String)

    @Serializer(forClass = Payload::class)
    object PayloadSerializer

    object BinaryPayloadSerializer : KSerializer<Payload> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BinaryPayload", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Payload) {
            encoder.encodeString(InternalHexConverter.printHexBinary(value.s.encodeToByteArray()))
        }

        override fun deserialize(decoder: Decoder): Payload {
            return Payload(InternalHexConverter.parseHexBinary(decoder.decodeString()).decodeToString())
        }
    }

    private fun SerialDescriptor.inContext(module: SerializersModule): SerialDescriptor = when (kind) {
        SerialKind.CONTEXTUAL -> requireNotNull(module.getContextualDescriptor(this)) { "Expected $this to be registered in module" }
        else -> error("Expected this function to be called on CONTEXTUAL descriptor")
    }

    @Test
    fun testResolveContextualDescriptor() {
        val simpleModule = serializersModuleOf(PayloadSerializer)
        val binaryModule = serializersModuleOf(BinaryPayloadSerializer)

        val contextDesc = EnhancedData.serializer().descriptor.elementDescriptors.toList()[1] // @ContextualSer stringPayload
        assertEquals(SerialKind.CONTEXTUAL, contextDesc.kind)
        assertEquals(0, contextDesc.elementsCount)

        val resolvedToDefault = contextDesc.inContext(simpleModule)
        assertEquals(StructureKind.CLASS, resolvedToDefault.kind)
        assertEquals("Payload", resolvedToDefault.serialName)
        assertEquals(1, resolvedToDefault.elementsCount)

        val resolvedToBinary = contextDesc.inContext(binaryModule)
        assertEquals(PrimitiveKind.STRING, resolvedToBinary.kind)
        assertEquals("BinaryPayload", resolvedToBinary.serialName)
    }
}
