/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test

class JsonPolymorphicObjectTest : JsonTestBase() {

    @Serializable
    data class Holder(@Polymorphic val a: Any)

    @Serializable
    @SerialName("MyObject")
    object MyObject {
        @Suppress("unused")
        val unused = 42
    }

    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(MyObject::class, MyObject.serializer()) // JS bug workaround
            }
        }
    }

    @Test
    fun testRegularPolymorphism() {
        assertJsonFormAndRestored(Holder.serializer(), Holder(MyObject), """{"a":{"type":"MyObject"}}""", json)
    }
}
