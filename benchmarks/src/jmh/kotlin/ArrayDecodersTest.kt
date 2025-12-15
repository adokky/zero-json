package dev.dokky.zerojson

import io.kodec.buffers.asArrayBuffer
import io.kodec.buffers.asBuffer
import kotlinx.serialization.json.decodeFromStream
import org.openjdk.jmh.annotations.*
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

@Measurement(iterations = 300, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 100, time = 100, timeUnit = TimeUnit.MILLISECONDS)
open class ArrayDecodersTest: BenchmarkBase() {
    @State(Scope.Thread)
    open class TLS : ThreadLocalStateBase(cacheInvalidation = CacheInvalidation.NONE)

    @Benchmark
    fun bytes_kotlinx(state: TLS): Any {
        return state.ktxJson.decodeFromStream<List<List<Float>>>(ByteArrayInputStream(TEST_DATA))
    }

    @Benchmark
    fun bytes_zjson_rc(state: TLS): Any {
        return state.zJson.decode<List<List<Float>>>(TEST_DATA.asArrayBuffer())
    }

    companion object {
        var TEST_DATA: ByteArray = Response.javaClass.getResourceAsStream("/test_array.json").readAllBytes()
        val buffer = TEST_DATA.asBuffer()
    }
}