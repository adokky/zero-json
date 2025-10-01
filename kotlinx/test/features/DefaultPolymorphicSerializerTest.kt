/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultPolymorphicSerializerTest : JsonTestBase() {

    @Serializable
    abstract class Project {
        abstract val name: String
    }

    @Serializable
    data class DefaultProject(override val name: String, val type: String): Project()

    val module = SerializersModule {
        polymorphic(Project::class) {
            defaultDeserializer { DefaultProject.serializer() }
        }
    }

    private val json = Json { serializersModule = module }

    @Test
    fun test() = parametrizedTest {
        assertEquals(
            DefaultProject("example", "unknown"),
            json.decodeFromString<Project>(""" {"type":"unknown","name":"example"}""", it))
    }

}
