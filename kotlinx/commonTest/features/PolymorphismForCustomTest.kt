/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals

class PolymorphismForCustomTest : JsonTestBase() {

    private val customSerializer = object : KSerializer<VImpl> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("VImpl") {
                element("a", String.serializer().descriptor)
            }

        override fun deserialize(decoder: Decoder): VImpl {
            decoder as JsonDecoder
            val jsonObject = decoder.decodeJsonElement() as JsonObject
            return VImpl(
                (jsonObject["a"] as JsonPrimitive).content
            )
        }

        override fun serialize(encoder: Encoder, value: VImpl) {
            encoder as JsonEncoder
            encoder.encodeJsonElement(
                JsonObject(mapOf("a" to JsonPrimitive(value.a)))
            )
        }
    }

    @Serializable
    data class ValueHolder<V : Any>(
        @Polymorphic val value: V,
    )

    data class VImpl(val a: String)

    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Any::class, VImpl::class, customSerializer)
        }
    }

    @Test
    fun test() = parametrizedTest { mode ->
        val valueHolder = ValueHolder(VImpl("aaa"))
        val encoded = json.encodeToString(ValueHolder.serializer(customSerializer), valueHolder, mode)
        assertEquals("""{"value":{"type":"VImpl","a":"aaa"}}""", encoded)

        val decoded = json.decodeFromString<ValueHolder<*>>(ValueHolder.serializer(customSerializer), encoded, mode)

        assertEquals(valueHolder, decoded)
    }

}
