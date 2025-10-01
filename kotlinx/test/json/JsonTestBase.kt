/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.assertEquals
import kotlin.test.assertTrue


enum class JsonTestingMode {
    STREAMING,
    TREE;
}

abstract class JsonTestBase {
    protected val default = Json { encodeDefaults = true }
    protected val lenient = Json { isLenient = true; ignoreUnknownKeys = true; allowSpecialFloatingPointValues = true }

    internal inline fun <reified T : Any> Json.encodeToString(
        value: T,
        jsonTestingMode: JsonTestingMode
    ): String {
        val serializer = serializersModule.serializer<T>()
        return encodeToString(serializer, value, jsonTestingMode)
    }

    internal fun <T> Json.encodeToString(
        serializer: SerializationStrategy<T>,
        value: T,
        jsonTestingMode: JsonTestingMode
    ): String =
        when (jsonTestingMode) {
            JsonTestingMode.STREAMING -> {
                encodeToString(serializer, value)
            }
            JsonTestingMode.TREE -> {
                val tree = encodeToJsonElement(serializer, value)
                encodeToString(tree)
            }
        }

    internal inline fun <reified T : Any> Json.decodeFromString(source: String, jsonTestingMode: JsonTestingMode): T {
        val deserializer = serializersModule.serializer<T>()
        return decodeFromString(deserializer, source, jsonTestingMode)
    }

    internal fun <T> Json.decodeFromString(
        deserializer: DeserializationStrategy<T>,
        source: String,
        jsonTestingMode: JsonTestingMode
    ): T =
        when (jsonTestingMode) {
            JsonTestingMode.STREAMING -> {
                decodeFromString(deserializer, source)
            }
            JsonTestingMode.TREE -> {
                val value = parseToJsonElement(source)
                decodeFromJsonElement(deserializer, value)
            }
        }

    protected open fun parametrizedTest(test: (JsonTestingMode) -> Unit) {
        processResults(buildList {
            add(runCatching { test(JsonTestingMode.STREAMING) })
            add(runCatching { test(JsonTestingMode.TREE) })
        })
    }

    private inner class SwitchableJson(
        val json: Json,
        val jsonTestingMode: JsonTestingMode,
        override val serializersModule: SerializersModule = EmptySerializersModule()
    ) : StringFormat {
        override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
            return json.encodeToString(serializer, value, jsonTestingMode)
        }

        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            return json.decodeFromString(deserializer, string, jsonTestingMode)
        }
    }

    protected fun parametrizedTest(json: Json, test: StringFormat.() -> Unit) {
        val streamingResult = runCatching { SwitchableJson(json, JsonTestingMode.STREAMING).test() }
        val treeResult = runCatching { SwitchableJson(json, JsonTestingMode.TREE).test() }
        processResults(listOf(streamingResult, treeResult))
    }

    protected fun processResults(results: List<Result<*>>) {
        results.forEachIndexed { i, result ->
            result.onFailure {
                println("Failed test for ${JsonTestingMode.entries[i]}")
                throw it
            }
        }
        for (i in results.indices) {
            for (j in results.indices) {
                if (i == j) continue
                assertEquals(
                    results[i].getOrNull()!!,
                    results[j].getOrNull()!!,
                    "Results differ for ${JsonTestingMode.entries[i]} and ${JsonTestingMode.entries[j]}"
                )
            }
        }
    }

    /**
     * Same as [assertStringFormAndRestored], but tests both json converters (streaming and tree)
     * via [parametrizedTest]
     */
    internal fun <T> assertJsonFormAndRestored(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        json: Json = default
    ) {
        parametrizedTest { jsonTestingMode ->
            val serialized = json.encodeToString(serializer, data, jsonTestingMode)
            assertEquals(expected, serialized, "Failed with streaming = $jsonTestingMode")
            val deserialized: T = json.decodeFromString(serializer, serialized, jsonTestingMode)
            assertEquals(data, deserialized, "Failed with streaming = $jsonTestingMode")
        }
    }
    /**
     * Same as [assertStringFormAndRestored], but tests both json converters (streaming and tree)
     * via [parametrizedTest]. Use custom checker for deserialized value.
     */
    internal fun <T> assertJsonFormAndRestoredCustom(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        check: (T, T) -> Boolean
    ) {
        parametrizedTest { jsonTestingMode ->
            val serialized = Json.encodeToString(serializer, data, jsonTestingMode)
            assertEquals(expected, serialized, "Failed with streaming = $jsonTestingMode")
            val deserialized: T = Json.decodeFromString(serializer, serialized, jsonTestingMode)
            assertTrue("Failed with streaming = $jsonTestingMode\n\tsource value =$data\n\tdeserialized value=$deserialized") { check(data, deserialized) }
        }
    }

    internal fun <T> assertRestoredFromJsonForm(
        serializer: KSerializer<T>,
        jsonForm: String,
        expected: T,
    ) {
        parametrizedTest { jsonTestingMode ->
            val deserialized: T = Json.decodeFromString(serializer, jsonForm, jsonTestingMode)
            assertEquals(expected, deserialized, "Failed with streaming = $jsonTestingMode")
        }
    }
}
