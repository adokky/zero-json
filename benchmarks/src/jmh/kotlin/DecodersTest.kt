package dev.dokky.zerojson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromStream
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.io.ByteArrayInputStream

@OptIn(ExperimentalSerializationApi::class)
open class DecodersTest: BenchmarkBase() {
    @State(Scope.Thread)
    open class TLS : ThreadLocalStateBase() {
        /** Discriminator In The Middle  */
        @Param("true", "false")
        var DITM: Boolean = false
    }

    @Benchmark
    fun bytes_kotlinx(state: TLS): Any {
        return state.ktxJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(state.copyOf(state.inputArray)))
    }

    @Benchmark
    fun bytes_kotlinx_no_copy(state: TLS): Any {
        return state.ktxJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(state.inputArray))
    }

    @Benchmark
    fun string_kotlinx(state: TLS): Any {
        return state.ktxJson.decodeFromString<Response<Person>>(state.inputString)
    }

    @Benchmark
    fun bytes_zjson(state: TLS): Any {
        return state.zJson.decodeFromByteArray<Response<Person>>(state.inputArray)
    }

    @Benchmark
    fun stream_zjson(state: TLS): Any {
        return state.zJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(state.inputArray))
    }

    @Benchmark
    fun string_zjson(state: TLS): Any {
        return state.zJson.decodeFromString<Response<Person>>(state.inputString)
    }
}

