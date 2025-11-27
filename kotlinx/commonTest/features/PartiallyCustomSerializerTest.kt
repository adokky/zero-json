/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable(WithNull.Companion::class)
data class WithNull(@SerialName("value") val nullable: String? = null) {
    @Serializer(forClass = WithNull::class)
    companion object : KSerializer<WithNull> {
        override fun serialize(encoder: Encoder, value: WithNull) {
            val elemOutput = encoder.beginStructure(descriptor)
            if (value.nullable != null) elemOutput.encodeStringElement(descriptor, 0, value.nullable)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Ignore // https://github.com/Kotlin/kotlinx.serialization/issues/2549
class PartiallyCustomSerializerTest {
    @Test
    fun partiallyCustom() {
        assertEquals("""{"value":"foo"}""", Json.encodeToString(WithNull.serializer(), WithNull("foo")))
        assertEquals("""{}""", Json.encodeToString(WithNull.serializer(), WithNull()))
    }
}
