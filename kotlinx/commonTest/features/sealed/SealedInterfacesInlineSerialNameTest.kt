/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.sealed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.serializer
import kotlin.test.Test

class SealedInterfacesInlineSerialNameTest : JsonTestBase() {
    @Serializable
    data class Child1Value(
        val a: Int,
        val b: String
    )

    @Serializable
    data class Child2Value(
        val c: Int,
        val d: String
    )

    @Serializable
    sealed interface Parent

    @Serializable
    @SerialName("child1")
    @JvmInline
    value class Child1(val value: Child1Value) : Parent

    @Serializable
    @SerialName("child2")
    @JvmInline
    value class Child2(val value: Child2Value) : Parent

    // From https://github.com/Kotlin/kotlinx.serialization/issues/2288
    @Test
    fun testSealedInterfaceInlineSerialName() {
        val messages = listOf(
            Child1(Child1Value(1, "one")),
            Child2(Child2Value(2, "two"))
        )
        assertJsonFormAndRestored(
            serializer(),
            messages,
            """[{"type":"child1","a":1,"b":"one"},{"type":"child2","c":2,"d":"two"}]"""
        )
    }
}
