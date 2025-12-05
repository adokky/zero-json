package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import dev.dokky.zerojson.framework.jvmOnly
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalClassesTest {
    object ObjectCustomSerializer: KSerializer<Any?> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Any?) {
            encoder.encodeNull()
        }

        override fun deserialize(decoder: Decoder): Any? {
            return decoder.decodeNull()
        }
    }

    class ClassCustomSerializer: KSerializer<Any?> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Any?) {
            encoder.encodeNull()
        }

        override fun deserialize(decoder: Decoder): Any? {
            return decoder.decodeNull()
        }
    }

    @Test
    fun testGeneratedSerializer() {
        @Serializable
        data class Local(val i: Int)

        val origin = Local(42)

        val decoded: Local = TestZeroJson.decodeFromString(TestZeroJson.encodeToString(origin))
        assertEquals(origin, decoded)
    }

    @Test
    fun testInLambda() {
        42.let {
            @Serializable
            data class Local(val i: Int)

            val origin = Local(it)

            val decoded: Local = TestZeroJson.decodeFromString(TestZeroJson.encodeToString(origin))
            assertEquals(origin, decoded)
        }
    }

    @Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
    @Test
    fun testObjectCustomSerializer() {
        @Serializable(with = ObjectCustomSerializer::class)
        data class Local(val i: Int)

        val origin: Local? = null

        val decoded: Local? = TestZeroJson.decodeFromString(TestZeroJson.encodeToString(origin))
        assertEquals(origin, decoded)
    }

    @Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
    @Test
    fun testClassCustomSerializer() {
        @Serializable(with = ClassCustomSerializer::class)
        data class Local(val i: Int)

        val origin: Local? = null

        // FIXME change to `noLegacyJs` when lookup of `ClassCustomSerializer` will work on Native and JS/IR
        jvmOnly {
            val decoded: Local? = TestZeroJson.decodeFromString(TestZeroJson.encodeToString(origin))
            assertEquals(origin, decoded)
        }
    }

}
