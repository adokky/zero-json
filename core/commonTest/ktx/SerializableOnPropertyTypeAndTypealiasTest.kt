package dev.dokky.zerojson.ktx

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test

@Serializable
private data class WithDefault(val s: String)

@Serializable(SerializerA::class)
private data class WithoutDefault(val s: String)

private object SerializerA : KSerializer<WithoutDefault> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithoutDefault) {
        encoder.encodeString(value.s)
    }

    override fun deserialize(decoder: Decoder): WithoutDefault {
        return WithoutDefault(decoder.decodeString())
    }
}

private object SerializerB : KSerializer<WithoutDefault> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithoutDefault) {
        encoder.encodeString(value.s + "#")
    }

    override fun deserialize(decoder: Decoder): WithoutDefault {
        return WithoutDefault(decoder.decodeString().removeSuffix("#"))
    }
}

private object SerializerC : KSerializer<WithDefault> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithDefault) {
        encoder.encodeString(value.s + "#")
    }

    override fun deserialize(decoder: Decoder): WithDefault {
        return WithDefault(decoder.decodeString().removeSuffix("#"))
    }
}

private typealias WithoutDefaultAlias = @Serializable(SerializerB::class) WithoutDefault
private typealias WithDefaultAlias = @Serializable(SerializerC::class) WithDefault

@Serializable
private data class TesterWithoutDefault(
    val b1: WithoutDefault,
    @Serializable(SerializerB::class) val b2: WithoutDefault,
    val b3: @Serializable(SerializerB::class) WithoutDefault,
    val b4: WithoutDefaultAlias
)

@Serializable
private data class TesterWithDefault(
    val b1: WithDefault,
    @Serializable(SerializerC::class) val b2: WithDefault,
    val b3: @Serializable(SerializerC::class) WithDefault,
    val b4: WithDefaultAlias
)

class SerializableOnPropertyTypeAndTypealiasTest : JsonTestBase() {

    @Test
    fun testWithDefault() {
        val t = TesterWithDefault(WithDefault("a"), WithDefault("b"), WithDefault("c"), WithDefault("d"))
        assertJsonFormAndRestored(
            TesterWithDefault.serializer(),
            t,
            """{"b1":{"s":"a"},"b2":"b#","b3":"c#","b4":"d#"}"""
        )
    }

    @Test
    fun testWithoutDefault() { // Ignored by #1895
        val t = TesterWithoutDefault(WithoutDefault("a"), WithoutDefault("b"), WithoutDefault("c"), WithoutDefault("d"))
        assertJsonFormAndRestored(
            TesterWithoutDefault.serializer(),
            t,
            """{"b1":"a","b2":"b#","b3":"c#","b4":"d#"}"""
        )
    }
}
