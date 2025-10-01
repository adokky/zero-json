package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertFailsWith

class UnknownElementIndexTest {
    @Suppress("unused")
    private enum class Choices { A, B, C }

    @Serializable
    private data class Holder(val c: Choices)

    private class MalformedReader : AbstractDecoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return UNKNOWN_NAME
        }
    }

    @Test
    fun testCompilerComplainsAboutIncorrectIndex() {
        assertFailsWith(SerializationException::class) {
            MalformedReader().decodeSerializableValue(Holder.serializer())
        }
    }

    @Test
    fun testErrorMessage() {
        val message = "kotlinx.serialization.UnknownElementIndexTest.Choices does not contain element with name 'D'"
        assertFailsWith(SerializationException::class, message) {
            ZeroJson.decodeFromString(Holder.serializer(), """{"c":"D"}""")
        }
    }
}
