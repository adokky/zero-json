/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.features.sealed.SealedChild
import kotlinx.serialization.features.sealed.SealedParent
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedClassesSerializationTest : JsonTestBase() {
    @Serializable
    sealed class SealedProtocol {
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
    sealed class ProtocolWithAbstractClass {

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
    sealed class ProtocolWithSealedClass {

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
    sealed class ProtocolWithGenericClass {

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
    data class SealedHolder(val s: SimpleSealed)

    @Serializable
    data class SealedBoxHolder(val b: Box<SimpleSealed>)

    private val json = Json

    @Test
    fun manualSerializer() {
        val message = json.encodeToString(
            ManualSerializer,
            SimpleSealed.SubSealedB(42)
        )
        assertEquals("{\"type\":\"kotlinx.serialization.SimpleSealed.SubSealedB\",\"i\":42}", message)
    }

    @Test
    fun onTopLevel() {
        val message = json.encodeToString(
            SimpleSealed.serializer(),
            SimpleSealed.SubSealedB(42)
        )
        assertEquals("{\"type\":\"kotlinx.serialization.SimpleSealed.SubSealedB\",\"i\":42}", message)
    }

    @Test
    fun insideClass() {
        assertJsonFormAndRestored(
            SealedHolder.serializer(),
            SealedHolder(SimpleSealed.SubSealedA("foo")),
            """{"s":{"type":"kotlinx.serialization.SimpleSealed.SubSealedA","s":"foo"}}""",
            json
        )
    }

    @Test
    fun insideGeneric() {
        assertJsonFormAndRestored(
            Box.serializer(SimpleSealed.serializer()),
            Box(SimpleSealed.SubSealedA("foo")),
            """{"boxed":{"type":"kotlinx.serialization.SimpleSealed.SubSealedA","s":"foo"}}""",
            json
        )
        assertJsonFormAndRestored(
            SealedBoxHolder.serializer(),
            SealedBoxHolder(Box(SimpleSealed.SubSealedA("foo"))),
            """{"b":{"boxed":{"type":"kotlinx.serialization.SimpleSealed.SubSealedA","s":"foo"}}}""",
            json
        )
    }

    @Test
    fun complexProtocol() {
        val messages = listOf(
            SealedProtocol.StringMessage("string message", "foo"),
            SealedProtocol.IntMessage("int message", 42),
            SealedProtocol.ErrorMessage("requesting termination"),
            SealedProtocol.EOF
        )
        val expected =
            """[{"type":"SealedProtocol.StringMessage","description":"string message","message":"foo"},{"type":"SealedProtocol.IntMessage","description":"int message","message":42},{"type":"SealedProtocol.ErrorMessage","error":"requesting termination"},{"type":"EOF"}]"""
        assertJsonFormAndRestored(ListSerializer(SealedProtocol.serializer()), messages, expected, json)
    }

    @Test
    fun protocolWithAbstractClass() {
        val messages = listOf(
            ProtocolWithAbstractClass.Message.StringMessage("string message", "foo"),
            ProtocolWithAbstractClass.Message.IntMessage("int message", 42),
            ProtocolWithAbstractClass.ErrorMessage("requesting termination"),
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
        val json = Json {
            serializersModule = abstractContext
        }
        val expected =
            """[{"type":"ProtocolWithAbstractClass.Message.StringMessage","description":"string message","message":"foo"},{"type":"ProtocolWithAbstractClass.Message.IntMessage","description":"int message","message":42},{"type":"ProtocolWithAbstractClass.ErrorMessage","error":"requesting termination"},{"type":"EOF"}]"""
        assertJsonFormAndRestored(ListSerializer(ProtocolWithAbstractClass.serializer()), messages, expected, json)
    }

    @Test
    fun protocolWithSealedClass() {
        val messages = listOf(
            ProtocolWithSealedClass.Message.StringMessage("string message", "foo"),
            ProtocolWithSealedClass.Message.IntMessage("int message", 42),
            ProtocolWithSealedClass.ErrorMessage("requesting termination"),
            ProtocolWithSealedClass.EOF
        )
        val expected =
            """[{"type":"ProtocolWithSealedClass.Message.StringMessage","description":"string message","message":"foo"},{"type":"ProtocolWithSealedClass.Message.IntMessage","description":"int message","message":42},{"type":"ProtocolWithSealedClass.ErrorMessage","error":"requesting termination"},{"type":"EOF"}]"""
        assertJsonFormAndRestored(ListSerializer(ProtocolWithSealedClass.serializer()), messages, expected, json)
    }

    @Test
    fun partOfProtocolWithSealedClass() {
        val messages = listOf(
            ProtocolWithSealedClass.Message.StringMessage("string message", "foo"),
            ProtocolWithSealedClass.Message.IntMessage("int message", 42)
        )
        val expected =
            """[{"type":"ProtocolWithSealedClass.Message.StringMessage","description":"string message","message":"foo"},{"type":"ProtocolWithSealedClass.Message.IntMessage","description":"int message","message":42}]"""

        assertJsonFormAndRestored(ListSerializer(ProtocolWithSealedClass.serializer()), messages, expected, json)
        assertJsonFormAndRestored(ListSerializer(ProtocolWithSealedClass.Message.serializer()), messages, expected, json)
    }

    @Test
    fun testSerializerLookupForSealedClass() {
        val resSer = serializer<SealedProtocol>()
        assertEquals(SealedProtocol::class, (resSer as AbstractPolymorphicSerializer).baseClass)
    }

    @Test
    fun testClassesFromDifferentFiles() {
        assertJsonFormAndRestored(SealedParent.serializer(), SealedChild(5), """{"type":"first child","i":1,"j":5}""")
    }
}
