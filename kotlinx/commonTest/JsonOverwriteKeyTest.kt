package kotlinx.serialization

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonOverwriteKeyTest : JsonTestBase() {
    private val json = Json

    @Serializable
    data class WrappedMap<T>(val mp: Map<String, T>)

    @Test
    fun testLatestKeyInMap() {
        val parsed = json.decodeFromString(WrappedMap.serializer(Int.serializer()), """{"mp": { "x" : 23, "x" : 42, "y": 4 }}""")
        assertEquals(WrappedMap(mapOf("x" to 42, "y" to 4)), parsed)
    }

    @Test
    fun testLastestListValueInMap() {
        val parsed = json.decodeFromString(WrappedMap.serializer(ListSerializer(Int.serializer())), """{"mp": { "x" : [23], "x" : [42], "y": [4] }}""")
        assertEquals(WrappedMap(mapOf("x" to listOf(42), "y" to listOf(4))), parsed)
    }
}
