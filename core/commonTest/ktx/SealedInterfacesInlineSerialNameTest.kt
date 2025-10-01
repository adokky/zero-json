package dev.dokky.zerojson.ktx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.jvm.JvmInline
import kotlin.test.Test

class SealedInterfacesInlineSerialNameTest : JsonTestBase() {
    @Serializable
    private data class Child1Value(
        val a: Int,
        val b: String
    )

    @Serializable
    private data class Child2Value(
        val c: Int,
        val d: String
    )

    @Serializable
    private sealed interface Parent

    @Serializable
    @SerialName("child1")
    @JvmInline
    private value class Child1(val value: Child1Value) : Parent

    @Serializable
    @SerialName("child2")
    @JvmInline
    private value class Child2(val value: Child2Value) : Parent

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
