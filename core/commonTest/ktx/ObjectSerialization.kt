package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectSerializationTest : JsonTestBase() {

    private sealed class ApiResponse {
        @Serializable
        @SerialName("ApiError")
        object Error : ApiResponse()

        @Serializable
        @SerialName("ApiResponse")
        data class Response(val message: String) : ApiResponse()
    }

    @Serializable
    private data class ApiCarrier(@Polymorphic val response: ApiResponse)

    private val module = SerializersModule {
        polymorphic(ApiResponse::class) {
            subclass(ApiResponse.Error.serializer())
            subclass(ApiResponse.Response.serializer())
        }
    }

    @Test
    fun testSealedClassSerialization() {
        val json = TestZeroJson { serializersModule = module }
        val carrier1 = ApiCarrier(ApiResponse.Error)
        val carrier2 = ApiCarrier(ApiResponse.Response("OK"))
        // {"response":{"type":"ApiError"}}
        assertJsonFormAndRestored(ApiCarrier.serializer(), carrier1, """{"response":{"type":"ApiError"}}""", json)
        assertJsonFormAndRestored(
            ApiCarrier.serializer(),
            carrier2,
            """{"response":{"type":"ApiResponse","message":"OK"}}""",
            json
        )
    }

    @Test
    fun testUnknownKeys() {
        val string = """{"metadata":"foo"}"""
        assertFailsWithMessage<SerializationException>("ignoreUnknownKeys") {
            TestZeroJson.decodeFromString(
                ApiResponse.Error.serializer(),
                string
            )
        }
        val json = TestZeroJson { ignoreUnknownKeys = true }
        assertEquals(ApiResponse.Error, json.decodeFromString(ApiResponse.Error.serializer(), string))
    }
}
