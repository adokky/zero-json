package dev.dokky.zerojson.ktx

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.elementDescriptors
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedDiamondTest : JsonTestBase() {
    @Serializable
    private sealed interface A

    @Serializable
    private sealed interface B : A

    @Serializable
    private sealed interface C : A

    @Serializable
    @SerialName("X")
    private data class X(val i: Int) : B, C

    @Serializable
    @SerialName("Y")
    private object Y : B, C

    @Suppress("unused")
    @SerialName("E")
    private enum class E : B, C { Q, W }

    @Test
    fun testMultipleSuperSealedInterfacesDescriptor() {
        assertEquals(
            listOf("E", "X", "Y"),
            A.serializer().descriptor.getElementDescriptor(1).elementDescriptors.map { it.serialName })
    }

    @Test
    fun testMultipleSuperSealedInterfaces() {
        @Serializable
        data class Carrier(val a: A, val b: B, val c: C)
        assertJsonFormAndRestored(
            Carrier.serializer(),
            Carrier(X(1), X(2), Y),
            """{"a":{"type":"X","i":1},"b":{"type":"X","i":2},"c":{"type":"Y"}}"""
        )
    }
}
