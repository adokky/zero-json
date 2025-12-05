package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import java.util.concurrent.CompletableFuture
import kotlin.test.Test

class MultiThreadingTest: RealWorldTestBase() {
    private val iterations = when(GlobalTestMode) {
        TestMode.QUICK -> 5
        TestMode.DEFAULT -> 10
        TestMode.FULL -> 20
    }

    private fun runTest(cacheMode: CacheMode) = repeat(iterations) {
        dev.dokky.zerojson.internal.DescriptorCache.SHARED_CACHES?.clear()
        val json = TestZeroJson { this.cacheMode = cacheMode }

        val futures = (1..Runtime.getRuntime().availableProcessors()).map {
            val subIterations = when(GlobalTestMode) {
                TestMode.QUICK -> 2
                TestMode.DEFAULT -> 8
                TestMode.FULL -> 20
            }

            CompletableFuture.runAsync {
                object : RandomizedJsonTest() {
                    fun test() {
                        val data: Response<Person> = Response(
                            data = (1..5).map { randomPerson() },
                            total = 2094,
                            version = 35353
                        )

                        randomizedTest {
                            domainObject(data)
                            jsonElement = jsonObject {
                                "data" array {
                                    for (item in data.data) {
                                        add(item.toJsonObject())
                                    }
                                }
                                "total" eq data.total
                                "version" eq data.version
                            }
                            this.json = json
                            this.iterations = subIterations
                        }
                    }
                }.test()
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
    }

    @Test
    fun shared() = runTest(CacheMode.SHARED)

    @Test
    fun two_level() = runTest(CacheMode.TWO_LEVEL)

    @Test
    fun exclusive() = runTest(CacheMode.THREAD_LOCAL)
}

