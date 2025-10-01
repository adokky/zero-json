package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.framework.transformers.CorruptionInputTransformer
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import dev.dokky.zerojson.framework.transformers.WrapperInputTransformer
import kotlinx.serialization.json.JsonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class TestConfigTest {
    private data class CustomTransformer(val id: Int): TestInputTransformer("Test transformer", TestTarget.decoders()) {
        override fun transform(input: MutableTestInput) { fail() }
    }

    @Test
    fun all_settings() {
        val cfg = TestConfig {
            domainObject(Unit)
            jsonElement = JsonNull
            iterations = 875
            json = ZeroJson { allowSpecialFloatingPointValues = true }
            compareToString = true
            name = "foo"
            exclude(TestTarget.TextToObject, TestTarget.BinaryToObject)
            exclude(TestTarget.TreeToObject)
            modesPerIteration = 988764
            transformers = mutableListOf(CorruptionInputTransformer, CustomTransformer(1), CustomTransformer(2))
            disableIf<CustomTransformer> { it.id < 2 }
            transformers += CustomTransformer(1)
            transformers += WrapperInputTransformer
        }

        assertEquals(true, cfg.compareToString)
        assertEquals(875, cfg.iterations)
        assertEquals("foo", cfg.name)
        assertEquals(true, cfg.json.configuration.allowSpecialFloatingPointValues)
        assertEquals(988764, cfg.modesPerIteration)
        assertEquals(setOf(TestTarget.TextToObject, TestTarget.BinaryToObject, TestTarget.TreeToObject), cfg.exclude)

        assertEquals(listOf(CorruptionInputTransformer, CustomTransformer(2), WrapperInputTransformer), cfg.transformers)
    }

    @Test
    fun disableTransformers() {
        val cfg = TestConfig {
            domainObject(Unit)
            jsonElement = JsonNull
            disable<RandomKeysInputTransformer>()
        }
        assertEquals(cfg.transformers, TestConfig.DefaultTransformers.filter { it !is RandomKeysInputTransformer })
    }
}