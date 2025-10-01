package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializerForNullableTypeTest : JsonTestBase() {

    // Nullable boxes
    @Serializable(with = StringHolderSerializer::class)
    private data class StringHolder(val s: String)

    private object StringHolderSerializer : KSerializer<StringHolder?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SHS", PrimitiveKind.STRING).nullable

        override fun serialize(encoder: Encoder, value: StringHolder?) {
            if (value == null) encoder.encodeString("nullable")
            else encoder.encodeString("non-nullable")
        }

        override fun deserialize(decoder: Decoder): StringHolder {
            if (decoder.decodeNotNullMark()) {
                return StringHolder("non-null: " + decoder.decodeString())
            }
            decoder.decodeNull()
            return StringHolder("nullable")
        }
    }

    @Serializable
    private data class Box(val s: StringHolder?)

    @Test
    fun testNullableBoxWithNotNull() {
        val b = Box(StringHolder("box"))
        val string = ZeroJson.encodeToString(b)
        assertEquals("""{"s":"non-nullable"}""", string)
        val deserialized = ZeroJson.decodeFromString<Box>(string)
        assertEquals(Box(StringHolder("non-null: non-nullable")), deserialized)
    }

    @Test
    fun testNullableBoxWithNull() {
        val b = Box(null)
        val string = ZeroJson.encodeToString(b)
        assertEquals("""{"s":"nullable"}""", string)
        val deserialized = ZeroJson.decodeFromString<Box>(string)
        assertEquals(Box(StringHolder("non-null: nullable")), deserialized)
    }

    @Test
    fun testNullableBoxDeserializeNull() {
        val deserialized = ZeroJson.decodeFromString<Box>("""{"s":null}""")
        assertEquals(Box(StringHolder("nullable")), deserialized)
    }

    // Nullable primitives
    object NullableLongSerializer : KSerializer<Long?> {
        @SerialName("NLS")
        @Serializable
        data class OptionalLong(val initialized: Boolean, val value: Long? = 0)

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NLS") {
            element<Boolean>("initialized")
            element<Long?>("value")
        }.nullable

        override fun serialize(encoder: Encoder, value: Long?) {
            val opt = OptionalLong(value != null, value)
            encoder.encodeSerializableValue(OptionalLong.serializer(), opt)
        }

        override fun deserialize(decoder: Decoder): Long? {
            val value = decoder.decodeSerializableValue(OptionalLong.serializer())
            return if (value.initialized) value.value else null
        }
    }

    @Serializable
    private data class NullablePrimitive(
        @Serializable(with = NullableLongSerializer::class) val value: Long?
    )

    @Test
    fun testNullableLongWithNotNull() {
        val data = NullablePrimitive(42)
        val json = ZeroJson.KtxCompat.encodeToString(data)
        assertEquals("""{"value":{"initialized":true,"value":42}}""", ZeroJson.KtxCompat.encodeToString(data))
        assertEquals(data, ZeroJson.KtxCompat.decodeFromString(json))
    }

    @Test
    fun testNullableLongWithNull() {
        val data = NullablePrimitive(null)
        val json = ZeroJson.KtxCompat.encodeToString(data)
        assertEquals("""{"value":{"initialized":false,"value":null}}""", ZeroJson.KtxCompat.encodeToString(data))
        assertEquals(data, ZeroJson.KtxCompat.decodeFromString(json))
    }

    // Now generics
    @Serializable
    private data class GenericNullableBox<T: Any>(val value: T?)

    @Serializable
    private data class GenericBox<T>(val value: T?)

    @Test
    fun testGenericBoxNullable() {
        val data = GenericBox<StringHolder?>(null)
        val json = ZeroJson.KtxCompat.encodeToString(data)
        assertEquals("""{"value":"nullable"}""", ZeroJson.KtxCompat.encodeToString(data))
        assertEquals(GenericBox(StringHolder("non-null: nullable")), ZeroJson.KtxCompat.decodeFromString(json))
    }

    @Test
    fun testGenericNullableBoxFromNull() {
        assertEquals(GenericBox(StringHolder("nullable")), ZeroJson.KtxCompat.decodeFromString("""{"value":null}"""))
    }

    @Test
    fun testGenericNullableBoxNullable() {
        val data = GenericNullableBox<StringHolder>(null)
        val json = ZeroJson.KtxCompat.encodeToString(data)
        assertEquals("""{"value":"nullable"}""", ZeroJson.KtxCompat.encodeToString(data))
        assertEquals(GenericNullableBox(StringHolder("non-null: nullable")), ZeroJson.KtxCompat.decodeFromString(json))
    }

    @Test
    fun testGenericBoxNullableFromNull() {
        assertEquals(GenericNullableBox(StringHolder("nullable")),
            ZeroJson.KtxCompat.decodeFromString("""{"value":null}"""))
    }
}
