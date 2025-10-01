package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.decodeFromJsonElement
import dev.dokky.zerojson.framework.assertFailsWithMessage
import karamel.utils.unsafeCast
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonElementDecodingTest : JsonTestBase() {
    @Serializable
    private data class A(val a: Int = 42)

    @Test
    fun testTopLevelClass() = assertSerializedForm(A(), """{}""".trimMargin())

    @Test
    fun testTopLevelNullableClass() {
        assertSerializedForm<A?>(A(), """{}""")
        assertSerializedForm<A?>(null, "null")
    }

    @Test
    fun testTopLevelPrimitive() = assertSerializedForm(42, """42""")

    @Test
    fun testTopLevelNullablePrimitive() {
        assertSerializedForm<Int?>(42, """42""")
        assertSerializedForm<Int?>(null, """null""")
    }

    @Test
    fun testTopLevelList() = assertSerializedForm(listOf(42), """[42]""")

    @Test
    fun testTopLevelNullableList() {
        assertSerializedForm<List<Int>?>(listOf(42), """[42]""")
        assertSerializedForm<List<Int>?>(null, """null""")
    }

    private val json = ZeroJson { explicitNulls = true; isLenient = false }

    private inline fun <reified T> assertSerializedForm(value: T, expectedString: String) {
        val element = Json.encodeToJsonElement(value)
        assertEquals(expectedString, element.toString())
        assertEquals(value, json.decodeFromJsonElement(element))
    }

    @Test
    fun testDeepRecursion() {
        // Reported as https://github.com/Kotlin/kotlinx.serialization/issues/1594
        // language=text
        var json = """{ "a": %}"""
        repeat(5) { json = json.replace("%", json) }
        json = json.replace("%", "0")
        ZeroJson.parseToJsonElement(json)
    }

    private open class NullAsElementSerializer<T : JsonElement>(private val serializer: KSerializer<out JsonElement>) : KSerializer<T?> {
        final override val descriptor: SerialDescriptor = serializer.descriptor.nullable

        final override fun serialize(encoder: Encoder, value: T?) {
            serializer.unsafeCast<KSerializer<JsonElement>>().serialize(encoder, value ?: JsonNull)
        }

        final override fun deserialize(decoder: Decoder): T = serializer.deserialize(decoder).unsafeCast<T>()
    }

    private object NullAsJsonNullJsonElementSerializer : NullAsElementSerializer<JsonElement>(dev.dokky.zerojson.JsonElementSerializer)
    private object NullAsJsonNullJsonPrimitiveSerializer : NullAsElementSerializer<JsonPrimitive>(dev.dokky.zerojson.JsonPrimitiveSerializer)
    private object NullAsJsonNullJsonNullSerializer : NullAsElementSerializer<JsonNull>(dev.dokky.zerojson.JsonPrimitiveSerializer)

    private val noExplicitNullsOrDefaultsJson = ZeroJson {
        explicitNulls = false
        encodeDefaults = false
    }
    private val explicitNullsJson = ZeroJson {
        explicitNulls = true
        encodeDefaults = true
    }

    @Test
    fun testNullableJsonElementDecoding() {
        @Serializable
        data class Wrapper(
            @Serializable(NullAsJsonNullJsonElementSerializer::class)
            val value: JsonElement? = null,
        )

        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = JsonNull), """{"value":null}""", explicitNullsJson)
        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = null), """{}""", noExplicitNullsOrDefaultsJson)
    }

    @Test
    fun testNullableJsonPrimitiveDecoding() {
        @Serializable
        data class Wrapper(
            @Serializable(NullAsJsonNullJsonPrimitiveSerializer::class)
            val value: JsonPrimitive? = null,
        )

        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = JsonNull), """{"value":null}""", explicitNullsJson)
        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = null), """{}""", noExplicitNullsOrDefaultsJson)
    }

    @Test
    fun testNullableJsonNullDecoding() {
        @Serializable
        data class Wrapper(
            @Serializable(NullAsJsonNullJsonNullSerializer::class)
            val value: JsonNull? = null,
        )

        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = JsonNull), """{"value":null}""", explicitNullsJson)
        assertJsonFormAndRestored(Wrapper.serializer(), Wrapper(value = null), """{}""", noExplicitNullsOrDefaultsJson)
    }

    @Test
    fun testLiteralIncorrectParsing() {
        val str = """{"a": "3 digit then random string"}"""
        val obj = json.decodeFromString(dev.dokky.zerojson.JsonObjectSerializer, str)
        assertFailsWithMessage<NumberFormatException>("Expected input to contain a single valid number") {
            obj.getValue("a").jsonPrimitive.long
        }
    }

    @Serializable
    private class NestedJsonObject<E: JsonElement>(val e: E)

    @Test
    fun nestedObject() {
        val obj = buildJsonObject {
            putJsonObject("nested") {
                put("k", "v")
            }
        }

        assertEquals("""{"e":{"nested":{"k":"v"}}}""", ZeroJson.encodeToString(
            NestedJsonObject.serializer(dev.dokky.zerojson.JsonElementSerializer),
            NestedJsonObject(obj))
        )
    }
}
