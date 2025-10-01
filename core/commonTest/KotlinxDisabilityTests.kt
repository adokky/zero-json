package dev.dokky.zerojson

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinxDisabilityTests {
    @Serializable
    sealed interface Base<T> {
        val field: T
    }

    @SerialName("A")
    @Serializable
    data class A(override val field: Int) : Base<Int>

    @SerialName("B")
    @Serializable
    data class B<T: CharSequence>(override val field: T) : Base<T>

    // seems like it fixed in kotlinx-serialization 1.9
//    @Test
//    @OptIn(InternalSerializationApi::class)
//    fun kotlinx_can_not_pass_type_argument_from_super_type() {
//        assertFails {
//            println(Json.encodeToString<Base<String>>(B("hello")))
//        }
//        assertFails {
//            println(Json.decodeFromString<Base<String>>("""{"type":"B","field":"hello"}"""))
//        }
//        assertFails {
//            println(Json.decodeFromString<Base<String>>("""{"type":"B","field":{"type":"kotlin.String","value":"hello"}}"""))
//        }
//    }

    @Test
    fun decoding_stupid_junk() {
        assertEquals(JsonUnquotedLiteral(""), Json.encodeToJsonElement(JsonElement.serializer(), JsonUnquotedLiteral("")))
    }
}