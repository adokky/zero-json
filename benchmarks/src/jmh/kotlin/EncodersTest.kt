package dev.dokky.zerojson

import io.kodec.buffers.ArrayBuffer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.encodeToStream
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalSerializationApi::class)
open class EncodersTest: BenchmarkBase() {
    @State(Scope.Thread)
    open class TLS : ThreadLocalStateBase() {
        lateinit var outputStream: ByteArrayOutputStream
        var outputBuf: ArrayBuffer = ArrayBuffer(ByteArray(BUF_SIZE), 0, BUF_SIZE)

        companion object {
            const val BUF_SIZE: Int = 40000
        }
    }

    @Benchmark
    fun bytes_kotlinx(state: TLS): Any {
        val output = ByteArrayOutputStream() // many re-allocations here
        state.ktxJson.encodeToStream<Response<Person>>(TEST_DATA, output)
        return output.toByteArray()
    }

    @Benchmark
    fun bytes_kotlinx_no_copy(state: TLS): Any {
        state.ktxJson.encodeToStream<Response<Person>>(TEST_DATA, state.outputStream)
        return state.outputStream
    }

    @Benchmark
    fun string_kotlinx(state: TLS): Any = state.ktxJson.encodeToString<Response<Person>>(TEST_DATA)

    @Benchmark
    fun tree_kotlinx(state: TLS): Any = state.ktxJson.encodeToJsonElement<Response<Person>>(TEST_DATA)

    @Benchmark
    fun bytes_zjson(state: TLS): Any = state.zJson.encodeToByteArray<Response<Person>>(TEST_DATA)

    @Benchmark
    fun bytes_zjson_no_copy(state: TLS): Any {
        state.zJson.encode<Response<Person>>(TEST_DATA, state.outputBuf)
        return state.outputBuf
    }

    @Benchmark
    fun string_zjson(state: TLS): Any = state.zJson.encodeToString<Response<Person>>(TEST_DATA)

    @Benchmark
    fun tree_zjson(state: TLS): Any = state.zJson.encodeToJsonElement<Response<Person>>(TEST_DATA)
}

