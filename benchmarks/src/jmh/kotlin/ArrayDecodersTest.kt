package dev.dokky.zerojson

import io.kodec.buffers.asArrayBuffer
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.io.ByteArrayInputStream

open class ArrayDecodersTest: BenchmarkBase() {
    @State(Scope.Thread)
    open class TLS : ThreadLocalStateBase(cacheInvalidation = CacheInvalidation.ALL_CORES) {
        var ser = serializer<List<List<Double>>>()
    }

    @Benchmark
    fun bytes_kotlinx(state: TLS): Any {
        return state.ktxJson.decodeFromStream(state.ser, ByteArrayInputStream(TEST_DATA))
    }

    @Benchmark
    fun bytes_zjson_rc(state: TLS): Any {
        return state.zJson.decodeFromBuffer(state.ser, TEST_DATA.asArrayBuffer())
    }

    companion object {
        var TEST_DATA: ByteArray = Response::class.java.getResourceAsStream("/test_array.json").readAllBytes()
    }
}