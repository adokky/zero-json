@file:Suppress("OPT_IN_USAGE")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJsonCompat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test

class JsonClassDiscriminatorTest : JsonTestBase() {
    @Serializable
    @JsonClassDiscriminator("sealedType")
    private sealed class SealedMessage {
        @Serializable
        @SerialName("SealedMessage.StringMessage")
        data class StringMessage(val description: String, val message: String) : SealedMessage()

        @SerialName("EOF")
        @Serializable
        object EOF : SealedMessage()
    }

    @Serializable
    @JsonClassDiscriminator("abstractType")
    private abstract class AbstractMessage {
        @Serializable
        @SerialName("Message.StringMessage")
        data class StringMessage(val description: String, val message: String) : AbstractMessage()

        @Serializable
        @SerialName("Message.IntMessage")
        data class IntMessage(val description: String, val message: Int) : AbstractMessage()
    }

    @Test
    fun testSealedClassesHaveCustomDiscriminator() {
        val messages = listOf(
            SealedMessage.StringMessage("string message", "foo"),
            SealedMessage.EOF
        )
        val expected = """[{"sealedType":"SealedMessage.StringMessage","description":"string message","message":"foo"},{"sealedType":"EOF"}]"""
        assertJsonFormAndRestored(
            ListSerializer(SealedMessage.serializer()),
            messages,
            expected,
        )
    }

    @Test
    fun testAbstractClassesHaveCustomDiscriminator() {
        val messages = listOf(
            AbstractMessage.StringMessage("string message", "foo"),
            AbstractMessage.IntMessage("int message", 42),
        )
        val module = SerializersModule {
            polymorphic(AbstractMessage::class) {
                subclass(AbstractMessage.StringMessage.serializer())
                subclass(AbstractMessage.IntMessage.serializer())
            }
        }
        val json = ZeroJsonCompat { serializersModule = module }
        val expected = """[{"abstractType":"Message.StringMessage","description":"string message","message":"foo"},{"abstractType":"Message.IntMessage","description":"int message","message":42}]"""
        assertJsonFormAndRestored(
            ListSerializer(AbstractMessage.serializer()), messages, expected, json
        )
    }

    @Serializable
    @JsonClassDiscriminator("message_type")
    private abstract class Base

    @Serializable
    private abstract class ErrorClass : Base()

    @Serializable
    private data class Message(val message: Base, val error: ErrorClass?)

    @Serializable
    @SerialName("my.app.BaseMessage")
    private data class BaseMessage(val message: String) : Base()

    @Serializable
    @SerialName("my.app.GenericError")
    private data class GenericError(@SerialName("error_code") val errorCode: Int) : ErrorClass()

    @Test
    fun testDocumentationInheritanceSample() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(BaseMessage.serializer())
            }
            polymorphic(ErrorClass::class) {
                subclass(GenericError.serializer())
            }
        }
        val json = ZeroJsonCompat { serializersModule = module }
        assertJsonFormAndRestored(
            Message.serializer(),
            Message(BaseMessage("not found"), GenericError(404)),
            """{"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}""",
            json
        )
    }
}
