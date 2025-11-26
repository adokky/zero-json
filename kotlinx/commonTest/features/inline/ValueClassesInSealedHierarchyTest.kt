/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.Test

class ValueClassesInSealedHierarchyTest : JsonTestBase() {
    @Test
    fun testSingle() {
        val single = "foo"
        assertJsonFormAndRestored(
            AnyValue.serializer(),
            AnyValue.Single(single),
            "\"$single\""
        )
    }

    @Test
    fun testComplex() {
        val complexJson = """{"id":"1","name":"object"}"""
        assertJsonFormAndRestored(
            AnyValue.serializer(),
            AnyValue.Complex(mapOf("id" to "1", "name" to "object")),
            complexJson
        )
    }

    @Test
    fun testMulti() {
        val multiJson = """["list","of","strings"]"""
        assertJsonFormAndRestored(
            AnyValue.serializer(),
            AnyValue.Multi(listOf("list", "of", "strings")),
            multiJson
        )
    }
}


// From https://github.com/Kotlin/kotlinx.serialization/issues/2159
@Serializable(with = AnyValue.Companion.Serializer::class)
sealed interface AnyValue {

    @JvmInline
    @Serializable
    value class Single(val value: String) : AnyValue

    @JvmInline
    @Serializable
    value class Multi(val values: List<String>) : AnyValue

    @JvmInline
    @Serializable
    value class Complex(val values: Map<String, String>) : AnyValue

    @JvmInline
    @Serializable
    value class Unknown(val value: JsonElement) : AnyValue

    companion object {
        object Serializer : JsonContentPolymorphicSerializer<AnyValue>(AnyValue::class) {

            override fun selectDeserializer(element: JsonElement): DeserializationStrategy<AnyValue> =
                when (element) {
                    is JsonArray if element.all { it is JsonPrimitive && it.isString } -> Multi.serializer()
                    is JsonObject if element.values.all { it is JsonPrimitive && it.isString } -> Complex.serializer()
                    is JsonPrimitive if element.isString -> Single.serializer()
                    else -> Unknown.serializer()
                }
        }
    }
}
