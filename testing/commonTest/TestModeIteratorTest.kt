@file:OptIn(InternalTestingApi::class)

package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.transformers.*
import karamel.utils.mapToSet
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.*

class TestModeIteratorTest {
    private val strategies5 = listOf(
        WrapperInputTransformer,
        RandomKeysInputTransformer,
        RandomOrderInputTransformer,
        RandomSpaceInputTransformer(),
        UnquotedStringsInputTransformer,
        CorruptionInputTransformer
    )

    @Test
    fun test() {
        val iterator = TestModeIterator(strategies5)

        var iterations = 0
        var firstDecoderModeProcessed = false
        val iteratedTargets = HashSet<TestTarget>()

        val strategyCombinations = (1 shl strategies5.size) - 1
        val minIterations = TestTarget.entries.size * (strategies5.size - 1)
        val maxIterations = strategyCombinations * TestTarget.entries.size

        iterator.forEachMode { mode ->
            assertEquals(0, mode.transformers.bits and (0.inv() shl strategies5.size))
            assertTrue(iterator.isValid(mode))

            iteratedTargets.add(mode.target)

            val selected = mode.transformers.toList(strategies5)

            // simple fixture check first
            if (mode.transformers.isTransformerPresent(strategies5, 5)) {
                assertEquals(TestTarget.DataType.Text, mode.target.input)
            }
            // then check all selected transformers automatically
            for (transformer in selected) {
                assertContains(transformer.targets, mode.target)
            }

            if (!firstDecoderModeProcessed && mode.target.isDecoder()) {
                // CorruptionTestTransformer should be skipped at first iteration
                // because it has the lowest priority and conflicts with some transformers
                assertEquals(0b111110, mode.transformers.bits, mode.transformers.bits.toString(2))
                firstDecoderModeProcessed = true
            }

            for (s1 in selected) {
                val s1Class = s1::class
                for (s2 in selected) {
                    if (s1Class in s2.incompatibleWith) fail("${s1.name} is incompatible with ${s2.name}")
                }
            }

            iterations++
            if (iterations > maxIterations) fail("too many iterations")
        }

        assertEquals(TestTarget.entries.toSet(), iteratedTargets)
        assertTrue(iterations > minIterations)
    }

    @Test
    fun exclusion() {
        val iterator = TestModeIterator(strategies5, exclude = setOf(TestTarget.TextToObject))
        assertEquals(
            (TestTarget.entries - TestTarget.TextToObject).toSet(),
            iterator.allModes().toSet().mapToSet { it.target }
        )
    }

    @Test
    fun iteration_count() {
        repeat(10) {
            val maxModes = Random.nextInt(800, 2000)
            val transformers = (1 .. Random.nextInt(10, 20)).map { WrapperInputTransformer }
            val iterator = TestModeIterator(transformers, maxModes = maxModes)

            val maxIterations = (maxModes * 1.6).roundToInt()

            repeat(100) {
                var i = 0
                val set = HashSet<JsonTestMode>()
                iterator.forEachMode {
                    assertTrue(set.add(it))
                    if (i >= maxIterations) fail("too many iterations: $i")
                    i++
                }
                assertEquals(set.size, i)
                assertTrue(i >= maxModes / 2, "too few iterations: $i")
            }
        }
    }
}

