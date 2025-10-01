package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithSerialMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPolymorphicSerializerTest {
    @Serializable
    private abstract class InvalidPolyRoot {
        abstract val name: String
    }
    @Serializable
    @SerialName("sub")
    private data class InvalidRuntimeSubClass(override val name: String, val type: String): InvalidPolyRoot()

    @Serializable
    private abstract class NormalPolyRoot {
        abstract val name: String
    }
    @Serializable
    private data class NormalRuntimeSubClass(override val name: String, val value: SimpleValueClass): NormalPolyRoot()

    private val module = SerializersModule {
        polymorphic(InvalidPolyRoot::class) {
            defaultDeserializer { InvalidRuntimeSubClass.serializer() }
        }
        polymorphic(NormalPolyRoot::class) {
            defaultDeserializer { NormalRuntimeSubClass.serializer() }
        }
    }

    private val json = ZeroJson { serializersModule = module; discriminatorConflict = DiscriminatorConflictDetection.SEALED }
    private val jsonAllConflict = ZeroJson(json) { discriminatorConflict = DiscriminatorConflictDetection.ALL }
    private val jsonNoConflict = ZeroJson(json) { discriminatorConflict = DiscriminatorConflictDetection.DISABLED }

    @Suppress("unused")
    @Serializable
    private sealed interface SealedConflict {
        @Serializable
        @SerialName("sealed-sub")
        class Sub(val type: String): SealedConflict

        @Serializable
        @SerialName("sealed-sub-no-conflict")
        @MaterializedDiscriminator
        data class SubNoConflict(val type: String): SealedConflict
    }
    
    private fun assertConflicted(body: () -> Unit) = assertFailsWithSerialMessage(
        exceptionName = "SerializationException",
        message = "conflicts with discriminator",
        block = body
    )

    private inline fun <reified T: Any> assertNoConflict(expected: T, input: String) {
        for (json in listOf(jsonNoConflict, json)) {
            assertEquals(expected, json.decodeFromString<T>(input))
        }
    }

    @Test
    fun discriminator_conflict_open()  {
        assertConflicted {
            jsonAllConflict.decodeFromString<InvalidPolyRoot>("""{"type":"sub","name":"example"}""")
        }
    }

    @Test
    fun discriminator_conflict_sealed()  {
        assertConflicted {
            json.decodeFromString<SealedConflict>("""{"type":"sealed-sub"}""")
        }
        assertConflicted {
            jsonAllConflict.decodeFromString<SealedConflict>("""{"type":"sealed-sub"}""")
        }
    }

    @Test
    fun no_discriminator_conflict_open()  {
        assertNoConflict<InvalidPolyRoot>(
            InvalidRuntimeSubClass(type = "unknown", name = "example"),
            """{"type":"unknown","name":"example"}"""
        )
    }

    @Test
    fun no_discriminator_conflict_sealed()  {
        assertNoConflict<SealedConflict>(
            SealedConflict.SubNoConflict(type = "sealed-sub-no-conflict"),
            """{"type":"sealed-sub-no-conflict"}"""
        )
    }

    @Test
    fun unknown_discriminator()  {
        assertEquals(
            NormalRuntimeSubClass("example", SimpleValueClass("СТРОКА")),
            json.decodeFromString<NormalPolyRoot>("""{"value":"СТРОКА","type":"unknown","name":"example"}""")
        )
    }
}
