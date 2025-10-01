package dev.dokky.zerojson.framework

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIsNot
import kotlin.test.assertTrue

class JsonObjectUtilsTest {
    @Test
    fun add_random_keys() {
        val obj = jsonObject { "key" eq "value" }
        val modified = obj.withRandomKeys(maxDepth = 1, minTopLevelKeys = 1)
        assertEquals("value", modified["key"]?.jsonPrimitive?.content)
        assertTrue(modified.entries.any { it.key != "key" }, "no random keys added")
    }

    @Test
    fun no_random_keys() {
        val obj = jsonObject { "key" eq "value" }
        val modified = obj.withRandomKeys(maxDepth = 0, minTopLevelKeys = 1)
        assertEquals("value", modified["key"]?.jsonPrimitive?.content)
        assertEquals(obj.size, modified.size)
    }

    private val iterations = if (GlobalTestMode == TestMode.QUICK) 10_000 else 100_000

    @Test
    fun no_nested_random_keys() = repeat(iterations) {
        val obj = jsonObject { "key" eq "value" }
        val modified = obj.withRandomKeys(maxDepth = 1, minTopLevelKeys = 1)
        assertEquals("value", modified["key"]?.jsonPrimitive?.content)

        for (entry in modified.entries) {
            if (entry.key == "key") continue
            assertIsNot<JsonObject>(entry.value)
            assertIsNot<JsonArray>(entry.value)
        }
    }

    @Test
    fun max_random_key_per_level() {
        val maxKeysPerLevel = 3

        var maxKeysNum = 0

        repeat(iterations) {
            val obj = jsonObject { "key" eq "value" }
            val modified = obj.withRandomKeys(maxDepth = 1, minTopLevelKeys = 1, maxKeysPerLevel = maxKeysPerLevel)
            assertEquals("value", modified["key"]?.jsonPrimitive?.content)

            val randomKeyCount = modified.entries.count { it.key != "key" }
            if (randomKeyCount > maxKeysNum) maxKeysNum = randomKeyCount
            assertTrue(randomKeyCount in 1..maxKeysPerLevel, "invalid number of random keys: $randomKeyCount")
        }

        assertEquals(maxKeysPerLevel, maxKeysNum)
    }
}