@file:OptIn(ExperimentalUuidApi::class)

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
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
    private data class Holder(val uuid: Uuid)

    @Serializable
    private data class HolderContextual(@Contextual val uuid: Uuid)

    @Test
    fun testCompiled() {
        val fixed = Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1")
        assertJsonFormAndRestored(
            Holder.serializer(),
            Holder(fixed),
            """{"uuid":"bc501c76-d806-4578-b45e-97a264e280f1"}""",
            ZeroJson
        )
    }

    @Test
    fun testContextual() {
        val fixed = Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1")
        assertJsonFormAndRestored(
            HolderContextual.serializer(),
            HolderContextual(fixed),
            """{"uuid":"bc501c76-d806-4578-b45e-97a264e280f1"}""",
            json = ZeroJson { serializersModule = serializersModuleOf(Uuid.serializer()) }
        )
    }
}
