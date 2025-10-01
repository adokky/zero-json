/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.sealed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.serializer
import kotlin.test.Test

class SealedInterfacesJsonSerializationTest : JsonTestBase() {
    @Serializable
    sealed interface I

    @Serializable
    sealed class Response: I {
        @Serializable
        @SerialName("ResponseInt")
        data class ResponseInt(val i: Int): Response()

        @Serializable
        @SerialName("ResponseString")
        data class ResponseString(val s: String): Response()
    }

    @Serializable
    @SerialName("NoResponse")
    object NoResponse: I

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
