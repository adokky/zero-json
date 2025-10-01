package dev.dokky.zerojson.ktx

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PolymorphicDeserializationErrorMessagesTest : JsonTestBase() {
    @Serializable
    private class DummyData(@Polymorphic val a: Any)

    @Serializable
    private class Holder(val d: DummyData)

    // TODO: remove this after #2480 is merged
    private fun checkSerializationException(action: () -> Unit, assertions: SerializationException.(String) -> Unit) {
        val e = assertFailsWith(SerializationException::class, action)
        assertNotNull(e.message)
        e.assertions(e.message!!)
    }

    @Test
    fun testNotRegisteredMessage() = parametrizedTest {
        val input = """{"d":{"a":{"type":"my.Class", "value":42}}}"""
        checkSerializationException({ default.decodeFromString<Holder>(input) }, { message ->
            assertContains(message, "Serializer for subclass 'my.Class' is not found in the polymorphic scope of 'Any'")
        })
    }

    @Test
    fun testDiscriminatorMissingNoDefaultMessage() = parametrizedTest {
        val input = """{"d":{"a":{"value":42}}}"""
        checkSerializationException({ default.decodeFromString<Holder>(input) }, { message ->
            assertContains(message, "Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Any'")
        })
    }

    @Test
    fun testClassDiscriminatorIsNull() = parametrizedTest {
        val input = """{"d":{"a":{"type":null, "value":42}}}"""
        checkSerializationException({ default.decodeFromString<Holder>(input) }, { message ->
            assertContains(message, "Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Any'")
        })
    }
}
