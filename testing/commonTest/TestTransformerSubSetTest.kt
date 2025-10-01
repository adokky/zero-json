@file:OptIn(InternalTestingApi::class)

package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.transformers.*
import kotlin.test.*

class TestTransformerSubSetTest {
    private val transformers = listOf(
        WrapperInputTransformer,
        RandomKeysInputTransformer,
        RandomOrderInputTransformer,
        CorruptionInputTransformer,
        UnquotedStringsInputTransformer,
        CorruptionInputTransformer
    )

    private fun List<TestInputTransformer>.select(vararg indices: Int): List<TestInputTransformer> =
        indices.map { this[it] }

    @Test
    fun empty() {
        val empty = SelectedTransformers.Empty
        assertEquals(0, empty.bits)
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty)
        transformers.indices.forEach { index ->
            assertFalse(empty.isTransformerPresent(transformers, index))
        }
        assertFalse(empty.isTransformerPresent(transformers, transformers.size))
        var iterated = 0
        empty.iterate(transformers) { iterated++ }
        assertEquals(0, iterated)
    }

    @Test
    fun all_strategies() {
        val ss = SelectedTransformers.all(transformers)
        assertEquals((1 shl transformers.size) - 1, ss.bits)
        assertEquals(transformers.size, ss.size)
        assertFalse(ss.isEmpty)
        transformers.indices.forEach { index ->
            assertTrue(ss.isTransformerPresent(transformers, index))
        }
        assertFalse(ss.isTransformerPresent(transformers, transformers.size))
        assertEquals(transformers, ss.toList(transformers))
    }

    @Test
    fun some_strategies() {
        val transformers = listOf(
            WrapperInputTransformer,
            RandomKeysInputTransformer,
            RandomOrderInputTransformer,
            RandomSpaceInputTransformer(),
            UnquotedStringsInputTransformer,
            CorruptionInputTransformer
        )

        assertEquals(transformers.select(1, 2, 3, 4), SelectedTransformers(0b011110).toList(transformers))
        assertEquals(transformers.select(0, 2, 3, 4), SelectedTransformers(0b101110).toList(transformers))
        assertEquals(transformers.select(1, 3),       SelectedTransformers(0b010100).toList(transformers))
    }

    @Test
    fun iterate_from() {
        fun SelectedTransformers.listFrom(from: Int) = buildList { iterateIndices(6, from = from) { add(it) } }
        assertEquals(listOf(2, 3, 4), SelectedTransformers(0b011110).listFrom(2))
        assertEquals(listOf(4, 5), SelectedTransformers(0b101111).listFrom(4))
        assertEquals(listOf(0, 2, 3, 4, 5), SelectedTransformers(0b101111).listFrom(0))
        assertEquals(listOf(2, 3, 4, 5), SelectedTransformers(0b101111).listFrom(1))
    }

    @Test
    fun modification() {
        val transformers = listOf(
            WrapperInputTransformer,
            RandomKeysInputTransformer,
            RandomOrderInputTransformer,
            CorruptionInputTransformer,
            UnquotedStringsInputTransformer,
            CorruptionInputTransformer
        )

        var ss = SelectedTransformers.all(transformers)

        assertEquals(ss.withTransformer(transformers, 5), ss)
        assertNotEquals(ss.withoutTransformer(transformers, 5), ss)

        ss = ss
            .withoutTransformer(transformers, 2)
            .withoutTransformer(transformers, 4)
            .withoutTransformer(transformers, 4)
            .withoutTransformer(transformers, 5)
            .withTransformer(transformers, 5)

        val selectedIndices = intArrayOf(0, 1, 3, 5)

        assertEquals(transformers.select(*selectedIndices), ss.toList(transformers))

        transformers.indices.forEach { i ->
            assertEquals(
                transformers[i].takeIf { i in selectedIndices },
                ss.getTransformerOrNull(transformers, i)
            )
        }
    }

    @Test
    fun create_from_indices() {
        val ss = SelectedTransformers(transformers, listOf(3, 4))
        assertEquals(transformers.select(3, 4), ss.toList(transformers))
    }

    @Test
    fun common_prefix_single() {
        assertEquals(0, SelectedTransformers(1).countCommonSignificantTransformers(SelectedTransformers(0), 6))
    }

    @Test
    fun common_prefix_simple() {
        val transformers = listOf(UnquotedStringsInputTransformer, UnquotedStringsInputTransformer)

        fun test(expected: Int, ss: SelectedTransformers) {
            val all = SelectedTransformers.all(transformers)
            assertEquals(expected, all.countCommonSignificantTransformers(ss, 2))
            assertEquals(expected, ss.countCommonSignificantTransformers(all, 2))
        }

        test(0, SelectedTransformers.Empty)
        test(1, SelectedTransformers(0b10))
        test(2, SelectedTransformers.all(transformers))
    }

    @Test
    fun common_prefix_simple_2() {
        fun test(expected: Int, ss: SelectedTransformers) {
            val all = SelectedTransformers(0b111)
            assertEquals(expected, all.countCommonSignificantTransformers(ss, 3))
            assertEquals(expected, ss.countCommonSignificantTransformers(all, 3))
        }

        test(0, SelectedTransformers.Empty)
        test(1, SelectedTransformers(0b101))
        test(0, SelectedTransformers(0b001))
        test(2, SelectedTransformers(0b110))
        test(0, SelectedTransformers(0b010))
    }

    @Test
    fun common_prefix_complex() {
        fun test(expected: Int, ss: SelectedTransformers) {
            val ss2 = SelectedTransformers(0b101)
            assertEquals(expected, ss2.countCommonSignificantTransformers(ss, 3))
            assertEquals(expected, ss.countCommonSignificantTransformers(ss2, 3))
        }

        test(0, SelectedTransformers.Empty)
        test(0, SelectedTransformers(0b001))
        test(0, SelectedTransformers(0b011))
        test(2, SelectedTransformers(0b101))
        test(1, SelectedTransformers(0b110))
        test(1, SelectedTransformers(0b111))
        test(1, SelectedTransformers(0b100))
    }
}