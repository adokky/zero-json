package dev.dokky.zerojson.ktx

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.builtins.TripleSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonGenericTest : JsonTestBase() {
    @Serializable
    private class Array2DBox(val arr: Array<Array<Double>>) {
        override fun toString() = arr.contentDeepToString()
    }

    @Test
    fun testWriteDefaultPair() = parametrizedTest {
        val pair = 42 to "foo"
        val serializer = PairSerializer(
            Int.serializer(),
            String.serializer()
        )
        val s = default.encodeToStringTest(serializer, pair)
        assertEquals("""{"first":42,"second":"foo"}""", s)
        val restored = default.decodeFromStringTest(serializer, s)
        assertEquals(pair, restored)
    }

    @Test
    fun testWritePlainTriple() = parametrizedTest {
        val triple = Triple(42, "foo", false)
        val serializer = TripleSerializer(
            Int.serializer(),
            String.serializer(),
            Boolean.serializer()
        )
        val s = default.encodeToStringTest(serializer, triple)
        assertEquals("""{"first":42,"second":"foo","third":false}""", s)
        val restored = default.decodeFromStringTest(serializer, s)
        assertEquals(triple, restored)
    }

    @Test
    fun testRecursiveArrays() = parametrizedTest {
        val arr = Array2DBox(arrayOf(arrayOf(2.1, 1.2), arrayOf(42.3, -3.4)))
        val str = default.encodeToStringTest(Array2DBox.serializer(), arr)
        assertEquals("""{"arr":[[2.1,1.2],[42.3,-3.4]]}""", str)
        val restored = default.decodeFromStringTest(Array2DBox.serializer(), str)
        assertTrue(arr.arr.contentDeepEquals(restored.arr))
    }
}
