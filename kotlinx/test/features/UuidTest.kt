/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalUuidApi::class)

package kotlinx.serialization.features

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.modules.serializersModuleOf
import kotlin.test.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UuidTest : JsonTestBase() {
    @Test
    fun testPlainUuid() {
        val uuid = Uuid.random()
        assertJsonFormAndRestored(Uuid.serializer(), uuid, "\"$uuid\"")
    }

    @Serializable
    data class Holder(val uuid: Uuid)

    @Serializable
    data class HolderContextual(@Contextual val uuid: Uuid)

    val json = Json { serializersModule = serializersModuleOf(Uuid.serializer()) }

    @Test
    fun testCompiled() {
        val fixed = Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1")
        assertJsonFormAndRestored(
            Holder.serializer(),
            Holder(fixed),
            """{"uuid":"bc501c76-d806-4578-b45e-97a264e280f1"}""",
            Json
        )
    }

    @Test
    fun testContextual() {
        val fixed = Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1")
        assertJsonFormAndRestored(
            HolderContextual.serializer(),
            HolderContextual(fixed),
            """{"uuid":"bc501c76-d806-4578-b45e-97a264e280f1"}""",
            json
        )
    }
}
