package karamel.benchmarks

import dev.dokky.zerojson.CacheMode
import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream

private val TEST_DATA = Response<Person>(
    data = (1..100).map { randomPerson() },
    total = 2094,
    version = 35353
)

private val ENCODED_DATA = ZeroJson.encodeToByteArray(TEST_DATA)

private val zJsonNonShared = ZeroJson { cacheMode = CacheMode.NON_SHARED }

private val zJsonTwoLevel = ZeroJson { cacheMode = CacheMode.TWO_LEVEL }

fun json5Benchmark(state: BenchmarkRunner.ThreadLocalState): Any {
    return ZeroJson.decodeFromByteArray<Response<Person>>(ENCODED_DATA)
}

fun json5BenchmarkTwoLevel(state: BenchmarkRunner.ThreadLocalState): Any {
    return zJsonTwoLevel.decodeFromByteArray<Response<Person>>(ENCODED_DATA)
}

fun json5BenchmarkThreadLocal(state: BenchmarkRunner.ThreadLocalState): Any {
    return zJsonNonShared.decodeFromByteArray<Response<Person>>(ENCODED_DATA)
}

@ExperimentalSerializationApi
fun ktxBenchmark(state: BenchmarkRunner.ThreadLocalState): Any {
    return state.ktxJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(ENCODED_DATA.copyOf()))
}

@ExperimentalSerializationApi
fun ktxBenchmarkNoCopy(state: BenchmarkRunner.ThreadLocalState): Any {
    return state.ktxJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(ENCODED_DATA))
}

fun createKotlinxJson(): Json = Json {
    explicitNulls = false
    isLenient = true
    @Suppress("OPT_IN_USAGE")
    allowComments = true
    allowStructuredMapKeys = true
}