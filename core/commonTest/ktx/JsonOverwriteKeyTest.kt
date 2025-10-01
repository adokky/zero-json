package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.ZeroJsonDecodingException
import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonOverwriteKeyTest : JsonTestBase() {
    @Serializable
    private data class Data(val a: Int)

    @Serializable
    private data class Updatable(val d: Data)

    @Test
    fun testLatestValueWins() {
        assertFailsWithMessage<ZeroJsonDecodingException>("key 'd' encountered multiple times") {
            default.decodeFromString<Updatable>("""{"d":{"a":"42"},"d":{"a":43}}""")
        }
    }

    @Serializable
    private data class WrappedMap<T>(val mp: Map<String, T>)

    @Test
    fun testLatestKeyInMap() {
        val parsed = ZeroJson.KtxCompat.decodeFromString(
            WrappedMap.serializer(Int.serializer()),
            """{"mp": { "x" : 23, "x" : 42, "y": 4 }}"""
        )
        assertEquals(WrappedMap(mapOf("x" to 42, "y" to 4)), parsed)
    }

    @Test
    fun testLatestListValueInMap() {
        val parsed = ZeroJson.KtxCompat.decodeFromString(
            WrappedMap.serializer(ListSerializer(Int.serializer())),
            """{"mp": { "x" : [23], "x" : [42], "y": [4] }}"""
        )
        assertEquals(WrappedMap(mapOf("x" to listOf(42), "y" to listOf(4))), parsed)
    }
}
