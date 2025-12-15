package dev.dokky.zerojson

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Threads

@Threads(4)
open class DecodersTestMultiThreaded : DecodersTest() {
    @Benchmark
    fun bytes_zjson_thread_local(state: TLS): Any {
        return zJsonNonShared.decodeFromByteArray(state.serializer, state.inputArray)
    }

    @Benchmark
    fun bytes_zjson_two_level(state: TLS): Any {
        return zJsonTwoLevel.decodeFromByteArray(state.serializer, state.inputArray)
    }

    companion object {
        private val zJsonNonShared = ZeroJson { cacheMode = CacheMode.THREAD_LOCAL }

        private val zJsonTwoLevel = ZeroJson { cacheMode = CacheMode.TWO_LEVEL }
    }
}