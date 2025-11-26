/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith

@Ignore("zero-json supports all the cases")
class JsonProhibitedPolymorphicKindsTest : JsonTestBase() {

    @Serializable
    sealed class Base {
        @Serializable
        class Impl(val data: Int) : Base()
    }

    @Serializable
    enum class MyEnum

    @Test
    fun testSealedSubclass() {
        assertFailsWith<IllegalArgumentException> {
            testJson {
                subclass(Base::class)
            }
        }
    }

    @Test
    fun testPrimitive() {
        assertFailsWith<IllegalArgumentException> {
            testJson {
                subclass(Int::class)
            }
        }
    }

    @Test
    fun testEnum() {
        assertFailsWith<IllegalArgumentException> {
            testJson {
                subclass(MyEnum::class)
            }
        }
    }

    @Test
    fun testStructures() {
        assertFailsWith<IllegalArgumentException> {
            testJson {
                subclass(serializer<Map<Int, Int>>())
            }
        }

        assertFailsWith<IllegalArgumentException> {
            testJson {
                subclass(serializer<List<Int>>())
            }
        }
    }

    private fun testJson(builderAction: PolymorphicModuleBuilder<Any>.() -> Unit) = Json {
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                builderAction()
            }
        }
    }
}
