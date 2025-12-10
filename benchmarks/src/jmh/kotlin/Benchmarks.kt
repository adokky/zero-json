package karamel.benchmarks

import dev.dokky.zerojson.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private var TEST_DATA = Response<Person>(
    data = (1..100).map { randomPerson() },
    total = 2094,
    version = 35353
)

private var ENCODED_DATA = ZeroJson.encodeToByteArray(TEST_DATA)

private var ENCODED_DATA_STRING = ZeroJson.encodeToString(TEST_DATA)

private var ENCODED_DATA_TREE = ZeroJson.encodeToJsonElement(TEST_DATA)

private val zJsonNonShared = ZeroJson { cacheMode = CacheMode.THREAD_LOCAL }

private val zJsonTwoLevel = ZeroJson { cacheMode = CacheMode.TWO_LEVEL }

fun decodeBytesZeroJson(state: ThreadLocalState): Any {
    return state.zJson.decodeFromByteArray<Response<Person>>(ENCODED_DATA)
}

fun decodeTreeZeroJson(state: ThreadLocalState): Any {
    return state.zJson.decodeFromJsonElement<Response<Person>>(ENCODED_DATA_TREE)
}

fun decodeStringZeroJson(state: ThreadLocalState): Any {
    return state.zJson.decodeFromString<Response<Person>>(ENCODED_DATA_STRING)
}

fun decodeBytesZeroJsonTwoLevel(state: ThreadLocalState): Any {
    return zJsonTwoLevel.decodeFromByteArray<Response<Person>>(ENCODED_DATA)
}

fun decodeBytesZeroJsonThreadLocal(state: ThreadLocalState): Any {
    return zJsonNonShared.decodeFromByteArray<Response<Person>>(ENCODED_DATA)
}

fun decodeInputStreamZeroJson(state: ThreadLocalState): Any {
    return state.zJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(ENCODED_DATA))
}

fun encodeBytesZeroJson(state: ThreadLocalState): Any {
    return state.zJson.encodeToByteArray(TEST_DATA)
}

fun encodeBytesZeroJsonNoCopy(state: ThreadLocalState): Any {
    state.zJson.encode(TEST_DATA, state.buffer)
    return state.buffer
}

fun encodeStringZeroJson(state: ThreadLocalState): Any {
    return state.zJson.encodeToString(TEST_DATA)
}

fun encodeTreeZeroJson(state: ThreadLocalState): Any {
    return state.zJson.encodeToJsonElement(TEST_DATA)
}

@ExperimentalSerializationApi
fun decodeBytesKtx(state: ThreadLocalState): Any {
    return state.ktxJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(state.copyOf(ENCODED_DATA)))
}

@ExperimentalSerializationApi
fun decodeStringKtx(state: ThreadLocalState): Any {
    return state.ktxJson.decodeFromString<Response<Person>>(ENCODED_DATA_STRING)
}

@ExperimentalSerializationApi
fun decodeBytesKtxNoCopy(state: ThreadLocalState): Any {
    return state.ktxJson.decodeFromStream<Response<Person>>(ByteArrayInputStream(ENCODED_DATA))
}

@ExperimentalSerializationApi
fun decodeTreeKtx(state: ThreadLocalState): Any {
    return state.ktxJson.decodeFromJsonElement<Response<Person>>(ENCODED_DATA_TREE)
}

@ExperimentalSerializationApi
fun encodeBytesKtx(state: ThreadLocalState): Any {
    val output = ByteArrayOutputStream() // many re-allocations here
    state.ktxJson.encodeToStream(TEST_DATA, output)
    return output.toByteArray()
}

@ExperimentalSerializationApi
fun encodeBytesKtxNoCopy(state: ThreadLocalState): Any {
    state.ktxJson.encodeToStream(TEST_DATA, state.outputStream)
    return state.outputStream
}

@ExperimentalSerializationApi
fun encodeStringKtx(state: ThreadLocalState): Any {
    return state.ktxJson.encodeToString(TEST_DATA)
}

@ExperimentalSerializationApi
fun encodeTreeKtx(state: ThreadLocalState): Any {
    return state.ktxJson.encodeToJsonElement(TEST_DATA)
}

fun createKotlinxJson(): Json = Json {
    explicitNulls = false
}

fun main() {
    println(ENCODED_DATA_STRING)
}