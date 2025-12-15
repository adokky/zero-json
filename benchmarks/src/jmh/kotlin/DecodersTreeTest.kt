package dev.dokky.zerojson

import kotlinx.serialization.ExperimentalSerializationApi
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@OptIn(ExperimentalSerializationApi::class)
open class DecodersTreeTest: BenchmarkBase() {
    @State(Scope.Thread)
    open class TLS : ThreadLocalStateBase()

    @Benchmark
    fun kotlinx(state: TLS): Any =
        state.ktxJson.decodeFromJsonElement(state.serializer, DiscriminatorAtStart.ENCODED_DATA_TREE)

    @Benchmark
    fun zjson(state: TLS): Any =
        state.zJson.decodeFromJsonElement(state.serializer, DiscriminatorAtStart.ENCODED_DATA_TREE)
}

