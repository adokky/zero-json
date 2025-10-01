package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.framework.assertStringFormAndRestored
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlin.test.Test
import kotlin.test.assertEquals

class TuplesTest : JsonTestBase() {
    @Serializable
    private data class MyPair<K, V>(val k: K, val v: V)

    @Serializable
    private data class PairWrapper(val p: Pair<Int, String>)

    @Serializable
    private data class TripleWrapper(val t: Triple<Int, String, Boolean>)

    @Test
    fun testCustomPair() = assertStringFormAndRestored(
        """{"k":42,"v":"foo"}""",
        MyPair(42, "foo"),
        MyPair.serializer(
            Int.serializer(),
            String.serializer()
        ),
        lenient
    )

    @Test
    fun testStandardPair() = assertStringFormAndRestored(
        """{"p":{"first":42,"second":"foo"}}""",
        PairWrapper(42 to "foo"),
        PairWrapper.serializer(),
        lenient
    )

    @Test
    fun testStandardPairHasCorrectDescriptor() {
        val desc = PairWrapper.serializer().descriptor.getElementDescriptor(0)
        assertEquals(desc.serialName, "kotlin.Pair")
        assertEquals(
            desc.elementDescriptors.map(SerialDescriptor::kind),
            listOf(PrimitiveKind.INT, PrimitiveKind.STRING)
        )
    }

    @Test
    fun testStandardTriple() = assertStringFormAndRestored(
        """{"t":{"first":42,"second":"foo","third":false}}""",
        TripleWrapper(Triple(42, "foo", false)),
        TripleWrapper.serializer(),
        lenient
    )

    @Test
    fun testStandardTripleHasCorrectDescriptor() {
        val desc = TripleWrapper.serializer().descriptor.getElementDescriptor(0)
        assertEquals(desc.serialName, "kotlin.Triple")
        assertEquals(
            desc.elementDescriptors.map(SerialDescriptor::kind),
            listOf(PrimitiveKind.INT, PrimitiveKind.STRING, PrimitiveKind.BOOLEAN)
        )
    }
}
