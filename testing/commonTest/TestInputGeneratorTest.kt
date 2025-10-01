package dev.dokky.zerojson.framework

import dev.dokky.zerojson.framework.transformers.*
import io.kodec.buffers.asBuffer
import karamel.utils.enrichMessageOf
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TestInputGeneratorTest {
    private val allTransformers = listOf(
        WrapperInputTransformer,
        RandomKeysInputTransformer,
        RandomOrderInputTransformer,
        RandomSpaceInputTransformer(),
        UnquotedStringsInputTransformer,
        CorruptionInputTransformer
    )

    private val simpleData = SimpleDataClass("hello")

    @Test
    fun generate_non_empty_input() {
        val cfg = TestConfig {
            domainObject(simpleData)
            jsonElement { "key" eq "hello" }
            transformers = allTransformers.toMutableList()
        }
        val gen = TestInputGenerator(cfg, StringBuilder())
        val exclude = allTransformers.indexOf(RandomOrderInputTransformer)
        val input = gen.generate(
            previousSelected = SelectedTransformers.all(allTransformers),
            selected = SelectedTransformers.all(allTransformers).withoutTransformer(allTransformers, exclude)
        )!!

        assertFalse(input.stringInput.isBlank())

        val expectedBuf = input.stringInput.encodeToByteArray().asBuffer()
        val gotBuffer = input.binaryInput.subBuffer(input.binaryOffset, input.binaryOffset + expectedBuf.size)
        assertEquals(expectedBuf, gotBuffer)
    }

    @Test
    fun generate_specific_input() {
        val strategySequence = listOf(UnquotedStringsInputTransformer)
        val cfg = TestConfig {
            domainObject(simpleData)
            jsonElement { "key" eq "hello" }
            transformers = strategySequence.toMutableList()
        }
        val gen = TestInputGenerator(cfg, StringBuilder())
        val input = gen.generate(
            previousSelected = SelectedTransformers.Empty,
            selected = SelectedTransformers.all(strategySequence)
        )

        input!!.checkStringInput("{key:hello}")
    }

    private fun TestInputImpl.checkStringInput(expected: String) {
        assertEquals(expected, stringInput)

        val expectedBuf = expected.encodeToByteArray().asBuffer()
        val actualBuf = binaryInput.subBuffer(binaryOffset, binaryOffset + expectedBuf.size)
        assertEquals(expectedBuf, actualBuf)
    }

    @Test
    fun transformation() {
        class AddSuffix(val suffix: String): TestInputTransformer("add suffix", TestTarget.entries.filter { it.isDecoder() }) {
            override fun transform(input: MutableTestInput) {
                input.transformTextInput { it.append(suffix) }
            }
        }

        val strategySequence = listOf(UnquotedStringsInputTransformer, AddSuffix(" A"), AddSuffix(" B"))
        val cfg = TestConfig {
            domainObject(simpleData)
            jsonElement { "key" eq "hello" }
            transformers = strategySequence.toMutableList()
        }
        val gen = TestInputGenerator(cfg, StringBuilder())

        val all = SelectedTransformers.all(strategySequence)
        gen.generate(SelectedTransformers.Empty, all)!!
            .checkStringInput("{key:hello} A B")

        val withB = SelectedTransformers(strategySequence, listOf(0, 2))
        gen.generate(all, withB)!!
            .checkStringInput("{key:hello} B")

        val withA = SelectedTransformers(strategySequence, listOf(0, 1))
        gen.generate(withB, withA)!!
            .checkStringInput("{key:hello} A")

        gen.generate(withA, all)!!
            .checkStringInput("{key:hello} A B")

        gen.generate(all, all)!!
            .checkStringInput("{key:hello} A B")
    }

    @Test
    fun iterating_all_modes() {
        val cfg = TestConfig {
            domainObject(Unit)
            jsonElement = JsonNull
            transformers = allTransformers.toMutableList()
        }
        val gen = TestInputGenerator(cfg, StringBuilder())

        var prev = JsonTestMode(TestTarget.entries.first(), SelectedTransformers(0))
        TestModeIterator(cfg.transformers, cfg.exclude).forEachMode { mode ->
            enrichMessageOf<Throwable>({ "$prev -> $mode" }) {
                gen.generate(prev.transformers, mode.transformers)
            }
            prev = mode
        }
    }
}
