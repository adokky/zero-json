package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.RandomOrderInputTransformer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StructuredKeysTest: RandomizedJsonTest() {
    private fun test(mode: StructuredMapKeysMode, customize: TestConfigBuilder.() -> Unit) {
        randomizedTest {
            json = TestZeroJson { structuredMapKeysMode = mode }
            domainObject(
                mapOf(
                    mapOf(
                        mapOf(
                            SimpleDataClass("key level 3") to SimpleDataClass("lv3"),
                            SimpleDataClass("\"ключ\"") to SimpleDataClass("ЗНАЧЕНИЕ")
                        ) to SimpleDataClass("lv2")
                    ) to SimpleDataClass("lv1"),
                )
            )
            customize()
        }
    }

    private fun sdc(key: String) = jsonObject { "key" eq key }

    private val lv41 = sdc("key level 3")
    private val lv42 = sdc("\"ключ\"")

    @Test
    fun garbage_at_the_end_structured_key() {
        assertFailsWith<SerializationException> {
            TestZeroJson.decodeFromString<Map<SimpleDataClass, String>>(
                """
                    {
                        "{\"key\":\"v\"}------" : "hello"
                    }
                """.trimIndent()
            )
        }
    }

    @Test
    fun escaping() {
        val lv3 = jsonObject(allowRandomKeys = false) {
            lv41.toString() eq sdc("lv3")
            lv42.toString() eq sdc("ЗНАЧЕНИЕ")
        }
        val lv2 = jsonObject(allowRandomKeys = false) { lv3.toString() eq sdc("lv2") }
        val lv1 = jsonObject(allowRandomKeys = false) { lv2.toString() eq sdc("lv1") }
        test(StructuredMapKeysMode.ESCAPED_STRING) {
            jsonElement = lv1
        }
    }

    @Test
    fun list_pair_without_value() {
        assertDecodingFails<Map<SimpleDataClass, String>>(
            jsonElement = buildJsonArray {
                addJsonObject {
                    put("key", "v")
                }
            },
            json = TestZeroJson { structuredMapKeysMode = StructuredMapKeysMode.LIST },
            message = "pairs following each other"
        )
    }

    @Test
    fun list() {
        val lv3 = buildJsonArray {
            add(lv41)
            add(sdc("lv3"))
            add(lv42)
            add(sdc("ЗНАЧЕНИЕ"))
        }
        val lv2 = buildJsonArray {
            add(lv3)
            add(sdc("lv2"))
        }
        val lv1 = buildJsonArray {
            add(lv2)
            add(sdc("lv1"))
        }
        test(StructuredMapKeysMode.LIST) {
            jsonElement = lv1
            disable<RandomOrderInputTransformer>()
        }
    }

    @Test
    fun map_of_value_classes() {
        val v = mapOf(SimpleValueInteger(1) to SimpleValueInteger(2))
        val encoded = ZeroJson.encodeToString(v)
        assertEquals("""{"1":2}""", encoded)
        assertEquals(v, ZeroJson.decodeFromString(encoded))
    }

    @Test
    fun map_of_lists() {
        val v = mapOf(listOf("1") to SimpleValueInteger(2))
        val encoded = ZeroJson.encodeToString(v)
        assertEquals("""[["1"],2]""", encoded)
        assertEquals(v, ZeroJson.decodeFromString(encoded))
    }
}