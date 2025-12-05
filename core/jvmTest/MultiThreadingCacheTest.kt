package dev.dokky.zerojson

import dev.dokky.zerojson.framework.GlobalTestMode
import dev.dokky.zerojson.framework.TestMode
import dev.dokky.zerojson.internal.DescriptorCache
import dev.dokky.zerojson.internal.ElementInfo
import dev.dokky.zerojson.internal.ZeroJsonDescriptor
import dev.dokky.zerojson.internal.unnestInline
import io.kodec.buffers.ArrayBuffer
import io.kodec.text.AbstractSubString
import io.kodec.text.StringTextReader
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import java.util.concurrent.CompletableFuture
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiThreadingCacheTest: RealWorldTestBase() {
    private val conf = TestZeroJson { cacheMode = CacheMode.SHARED }.configuration

    private val iterations: Int = when(GlobalTestMode) {
        TestMode.QUICK -> 10
        TestMode.DEFAULT -> 100
        TestMode.FULL -> 1000
    }

    @Test
    fun test() = repeat(iterations) { outerIteration ->
        DescriptorCache.SHARED_CACHES?.clear()
        val keys = listOf(
            "id",
            "name",
            "description",
            "department",
            "participation",
            "friends",
        )
        val keysConcat = keys.joinToString(",")
        val futures = (1.. Runtime.getRuntime().availableProcessors()).map {
            CompletableFuture.runAsync {
                actor(keysConcat, outerIteration, keys)
            }
        }
        CompletableFuture.allOf(*futures.toTypedArray()).join()
    }

    private fun actor(
        keysConcat: String,
        outerIteration: Int,
        keys: List<String>
    ) {
        val tempReader = StringTextReader()
        val keySequence = StringTextReader()
        keySequence.startReadingFrom(keysConcat)
        fun key(name: String): AbstractSubString {
            val start = keySequence.input.indexOf(name)
            return keySequence.substring(start, start + name.length)
        }

        repeat(100) { iteration ->
            if (Random.nextInt(7) == 0) DescriptorCache.SHARED_CACHES?.clear()
            val cache = DescriptorCache(conf)

            val desc = cache.getOrCreateUnsafe(Employee.serializer().descriptor)
            assertEquals(Employee.serializer().descriptor, desc.serialDescriptorUnsafe)

            fun ZeroJsonDescriptor.checkField(key: AbstractSubString, keySer: KSerializer<*>?) {
                var res = getElementInfoByName(key, tempBuf)
                if (!res.isRegular) {
                    var retries = 0
                    while (!res.isRegular && retries++ < 1) {
                        res = getElementInfoByName(key, tempBuf)
                    }
                    error("$key ${key.toString().hashCode()}, i=$outerIteration, j=$iteration, ${retries-1} retries")
                }
                if (keySer != null)
                    assertEquals(keySer.descriptor.unnestInline(), getElementDescriptor(res.index)!!.serialDescriptorUnsafe)
            }

            desc.checkField(key("id"), EmployeeId.serializer())
            desc.checkField(key("name"), String.serializer())
            desc.checkField(key("description"), String.serializer().nullable)
            desc.checkField(key("department"), Department.serializer())
            desc.checkField(key("participation"), null)
            desc.checkField(key("friends"), null)

            val size = Random.nextInt(20)
            val randomKey = buildString(size) {
                repeat(size) {
                    append(Random.nextInt(0..1000).toChar())
                }
            }
            tempReader.startReadingFrom(randomKey)
            val res = desc.getElementInfoByName(tempReader.substring(0, randomKey.length), tempBuf)
            if (randomKey !in keys) {
                assertEquals(ElementInfo(7, 6), res)
            }
        }
    }

    val tempBuf = ArrayBuffer(0)
}