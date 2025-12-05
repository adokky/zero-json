package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionSerializerTest {
    @Serializable
    private data class CollectionWrapper(
        val collection: Collection<String>
    )

    @Test
    fun testListJson() {
        val list = listOf("foo", "bar", "foo", "bar")

        val string = TestZeroJson.encodeToString(CollectionWrapper(list))
        assertEquals("""{"collection":["foo","bar","foo","bar"]}""", string)

        val wrapper = TestZeroJson.decodeFromString<CollectionWrapper>(string)
        assertEquals(list, wrapper.collection)
    }

    @Test
    fun testSetJson() {
        val set = setOf("foo", "bar", "foo", "bar")

        val string = TestZeroJson.encodeToString(CollectionWrapper(set))
        assertEquals("""{"collection":["foo","bar"]}""", string)

        val wrapper = TestZeroJson.decodeFromString<CollectionWrapper>(string)
        assertEquals(set.toList(), wrapper.collection)
    }
}
