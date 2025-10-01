package dev.dokky.zerojson.ktx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test

class SealedInterfacesJsonSerializationTest : JsonTestBase() {
    @Serializable
    private sealed interface I

    @Serializable
    private sealed class Response: I {
        @Serializable
        @SerialName("ResponseInt")
        data class ResponseInt(val i: Int): Response()

        @Serializable
        @SerialName("ResponseString")
        data class ResponseString(val s: String): Response()
    }

    @Serializable
    @SerialName("NoResponse")
    private object NoResponse: I

    @Test
    fun testSealedInterfaceJson() {
        val messages = listOf(Response.ResponseInt(10), NoResponse, Response.ResponseString("foo"))
        assertJsonFormAndRestored(
            serializer(),
            messages,
            """[{"type":"ResponseInt","i":10},{"type":"NoResponse"},{"type":"ResponseString","s":"foo"}]"""
        )
    }
}
