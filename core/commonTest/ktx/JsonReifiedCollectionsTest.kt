package dev.dokky.zerojson.ktx

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonReifiedCollectionsTest : JsonTestBase() {
    @Serializable
    data class DataHolder(val data: String)

    @Test
    fun testReifiedList() = parametrizedTest {
        val data = listOf(DataHolder("data"), DataHolder("not data"))
        val json = default.encodeToStringTest(data)
        val data2 = default.decodeFromStringTest<List<DataHolder>>(json)
        assertEquals(data, data2)
    }

    @Test
    fun testReifiedMap() = parametrizedTest {
        val data = mapOf("data" to DataHolder("data"), "smth" to DataHolder("not data"))
        val json = lenient.encodeToStringTest(data)
        val data2 = lenient.decodeFromStringTest<Map<String, DataHolder>>(json)
        assertEquals(data, data2)
    }
}
