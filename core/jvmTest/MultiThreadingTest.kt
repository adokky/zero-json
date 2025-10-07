package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import java.util.concurrent.CompletableFuture
import kotlin.test.Test

class MultiThreadingTest: RealWorldTestBase() {
    private fun runTest(cacheMode: CacheMode) {
        val data = Response(
            data = (1..5).map { randomPerson() },
            total = 2094,
            version = 35353
        )

        val iterations = when(GlobalTestMode) {
            TestMode.QUICK -> 30
            TestMode.DEFAULT -> 50
            TestMode.FULL -> 150
        }

        val json = ZeroJson { this.cacheMode = cacheMode }

        val futures = (1.. Runtime.getRuntime().availableProcessors()).map {
            CompletableFuture.runAsync {
                object : RandomizedJsonTest() {
                    fun test() = randomizedTest {
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
                        this.iterations = iterations
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
    fun exclusive() = runTest(CacheMode.NON_SHARED)
}