package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.*
import dev.dokky.zerojson.framework.trimMarginAndRemoveWhitespaces
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedClassesSerializationTest : JsonTestBase() {
    @Serializable
    private sealed class SealedProtocol {
        @Serializable
        @SerialName("SealedProtocol.StringMessage")
        data class StringMessage(val description: String, val message: String) : SealedProtocol()

        @Serializable
        @SerialName("SealedProtocol.IntMessage")
        data class IntMessage(val description: String, val message: Int) : SealedProtocol()

        @Serializable
        @SerialName("SealedProtocol.ErrorMessage")
        data class ErrorMessage(val error: String) : SealedProtocol()

        @SerialName("EOF")
        @Serializable
        object EOF : SealedProtocol()
    }

    @Serializable
    private sealed class ProtocolWithAbstractClass {
        @Serializable
        @SerialName("ProtocolWithAbstractClass.Message")
        abstract class Message : ProtocolWithAbstractClass() {
            @Serializable
            @SerialName("ProtocolWithAbstractClass.Message.StringMessage")
            data class StringMessage(val description: String, val message: String) : Message()

            @Serializable
            @SerialName("ProtocolWithAbstractClass.Message.IntMessage")
            data class IntMessage(val description: String, val message: Int) : Message()
        }

        @Serializable
        @SerialName("ProtocolWithAbstractClass.ErrorMessage")
        data class ErrorMessage(val error: String) : ProtocolWithAbstractClass()

        @SerialName("EOF")
        @Serializable
        object EOF : ProtocolWithAbstractClass()
    }

    @Serializable
    private sealed class ProtocolWithSealedClass {
        @Serializable
        @SerialName("ProtocolWithSealedClass.Message")
        sealed class Message : ProtocolWithSealedClass() {
            @Serializable
            @SerialName("ProtocolWithSealedClass.Message.StringMessage")
            data class StringMessage(val description: String, val message: String) : Message()

            @Serializable
            @SerialName("ProtocolWithSealedClass.Message.IntMessage")
            data class IntMessage(val description: String, val message: Int) : Message()
        }

        @Serializable
        @SerialName("ProtocolWithSealedClass.ErrorMessage")
        data class ErrorMessage(val error: String) : ProtocolWithSealedClass()

        @SerialName("EOF")
        @Serializable
        object EOF : ProtocolWithSealedClass()
    }

    @Serializable
    private sealed class ProtocolWithGenericClass {
        @Serializable
        @SerialName("ProtocolWithGenericClass.Message")
        data class Message<T>(val description: String, val message: T) : ProtocolWithGenericClass()

        @Serializable
        @SerialName("ProtocolWithGenericClass.ErrorMessage")
        data class ErrorMessage(val error: String) : ProtocolWithGenericClass()

        @SerialName("EOF")
        @Serializable
        object EOF : ProtocolWithGenericClass()
    }

    private val ManualSerializer: KSerializer<SimpleSealed> = SealedClassSerializer(
        "SimpleSealed",
        SimpleSealed::class,
        arrayOf(SimpleSealed.SubSealedA::class, SimpleSealed.SubSealedB::class),
        arrayOf(SimpleSealed.SubSealedA.serializer(), SimpleSealed.SubSealedB.serializer())
    )

    @Serializable
    private data class SealedHolder(val s: SimpleSealed)

    @Serializable
    private data class SealedBoxHolder(val b: Box<SimpleSealed>)

    @Test
    fun manualSerializer() {
        assertEquals(
            "{\"type\":\"dev.dokky.zerojson.SimpleSealed.SubSealedB\",\"i\":42}",
            ZeroJson.encodeToString(ManualSerializer, SimpleSealed.SubSealedB(42))
        )
    }

    @Test
    fun onTopLevel() {
        assertEquals(
            "{\"type\":\"dev.dokky.zerojson.SimpleSealed.SubSealedB\",\"i\":42}",
            ZeroJson.encodeToString(SimpleSealed.serializer(), SimpleSealed.SubSealedB(42))
        )
    }

    @Test
    fun insideClass() {
        assertJsonFormAndRestored(
            SealedHolder.serializer(),
            SealedHolder(SimpleSealed.SubSealedA("foo")),
            """{"s":{"type":"dev.dokky.zerojson.SimpleSealed.SubSealedA","s":"foo"}}""",
            ZeroJson
        )
    }

    @Test
    fun insideGeneric() {
        assertJsonFormAndRestored(
            Box.serializer(SimpleSealed.serializer()),
            Box<SimpleSealed>(SimpleSealed.SubSealedA("foo")),
            """{"value":{"type":"dev.dokky.zerojson.SimpleSealed.SubSealedA","s":"foo"}}""",
            ZeroJson
        )
        assertJsonFormAndRestored(
            SealedBoxHolder.serializer(),
            SealedBoxHolder(Box(SimpleSealed.SubSealedA("foo"))),
            """{"b":{"value":{"type":"dev.dokky.zerojson.SimpleSealed.SubSealedA","s":"foo"}}}""",
            ZeroJson
        )
    }

    @Test
    fun complexProtocol() {
        val messages = listOf<SealedProtocol>(
            SealedProtocol.StringMessage("string_message", "foo"),
            SealedProtocol.IntMessage("int_message", 42),
            SealedProtocol.ErrorMessage("requesting_termination"),
            SealedProtocol.EOF
        )
        val expected = """[
            |{"type":"SealedProtocol.StringMessage","description":"string_message","message":"foo"},
            |{"type":"SealedProtocol.IntMessage","description":"int_message","message":42},
            |{"type":"SealedProtocol.ErrorMessage","error":"requesting_termination"},
            |{"type":"EOF"}]""".trimMarginAndRemoveWhitespaces()
        assertJsonFormAndRestored(ListSerializer(SealedProtocol.serializer()), messages, expected, ZeroJson)
    }

    @Test
    fun protocolWithAbstractClass() {
        val messages = listOf<ProtocolWithAbstractClass>(
            ProtocolWithAbstractClass.Message.StringMessage("string_message", "foo"),
            ProtocolWithAbstractClass.Message.IntMessage("int_message", 42),
            ProtocolWithAbstractClass.ErrorMessage("requesting_termination"),
            ProtocolWithAbstractClass.EOF
        )
        val abstractContext = SerializersModule {
            polymorphic(ProtocolWithAbstractClass::class) {
                subclass(ProtocolWithAbstractClass.Message.IntMessage.serializer())
                subclass(ProtocolWithAbstractClass.Message.StringMessage.serializer())
            }
            polymorphic(ProtocolWithAbstractClass.Message::class) {
                subclass(ProtocolWithAbstractClass.Message.IntMessage.serializer())
                subclass(ProtocolWithAbstractClass.Message.StringMessage.serializer())
            }
        }
        val expected = """[
            |{"type":"ProtocolWithAbstractClass.Message.StringMessage","description":"string_message","message":"foo"},
            |{"type":"ProtocolWithAbstractClass.Message.IntMessage","description":"int_message","message":42},
            |{"type":"ProtocolWithAbstractClass.ErrorMessage","error":"requesting_termination"},
            |{"type":"EOF"}]""".trimMarginAndRemoveWhitespaces()
        assertJsonFormAndRestored(
            ListSerializer(ProtocolWithAbstractClass.serializer()),
            messages,
            expected,
            ZeroJson { serializersModule = abstractContext })
    }

    @Test
    fun protocolWithSealedClass() {
        val messages = listOf<ProtocolWithSealedClass>(
            ProtocolWithSealedClass.Message.StringMessage("string_message", "foo"),
            ProtocolWithSealedClass.Message.IntMessage("int_message", 42),
            ProtocolWithSealedClass.ErrorMessage("requesting_termination"),
            ProtocolWithSealedClass.EOF
        )
        val expected = """[
            |{"type":"ProtocolWithSealedClass.Message.StringMessage","description":"string_message","message":"foo"},
            |{"type":"ProtocolWithSealedClass.Message.IntMessage","description":"int_message","message":42},
            |{"type":"ProtocolWithSealedClass.ErrorMessage","error":"requesting_termination"},
            |{"type":"EOF"}]""".trimMarginAndRemoveWhitespaces()
        assertJsonFormAndRestored(ListSerializer(ProtocolWithSealedClass.serializer()), messages, expected,
            ZeroJson
        )
    }

    @Test
    fun partOfProtocolWithSealedClass() {
        val messages = listOf<ProtocolWithSealedClass.Message>(
            ProtocolWithSealedClass.Message.StringMessage("string_message", "foo"),
            ProtocolWithSealedClass.Message.IntMessage("int_message", 42)
        )
        val expected = """[
            |{"type":"ProtocolWithSealedClass.Message.StringMessage","description":"string_message","message":"foo"},
            |{"type":"ProtocolWithSealedClass.Message.IntMessage","description":"int_message","message":42}
            |]""".trimMarginAndRemoveWhitespaces()

        assertJsonFormAndRestored(
            ListSerializer(ProtocolWithSealedClass.serializer()),
            messages, expected, ZeroJson
        )
        assertJsonFormAndRestored(
            ListSerializer(ProtocolWithSealedClass.Message.serializer()),
            messages, expected, ZeroJson
        )
    }

    @Test
    fun testSerializerLookupForSealedClass() {
        val resSer = serializer<SealedProtocol>()
        assertEquals(SealedProtocol::class, (resSer as AbstractPolymorphicSerializer).baseClass)
    }

    @Test
    fun testClassesFromDifferentFiles() {
        assertJsonFormAndRestored(SealedParent.serializer(), SealedChild(5),
            """{"type":"first child","i":1,"j":5}""")
    }
}
