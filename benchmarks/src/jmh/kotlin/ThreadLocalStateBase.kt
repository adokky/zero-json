package dev.dokky.zerojson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.infra.Blackhole
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom

enum class CacheInvalidation {
    ALL_CORES,
    SINGLE_CORE,
    NONE
}

@OptIn(ExperimentalSerializationApi::class)
abstract class ThreadLocalStateBase(val cacheInvalidation: CacheInvalidation = CacheInvalidation.NONE) {
    private val shuffleBuf = ThreadLocal.withInitial { ByteArray(2 * 1024 * 1024) }

    var ktxJson: Json = Json {
        explicitNulls = false
    }
    var zJson: ZeroJson = ZeroJson(ktxJson.configuration, ktxJson.serializersModule)

    var serializer = serializer<Response<Person>>()

    private fun prepare() {
//         imitate complex business logic:
//         invalidate CPU caches by shuffling large array
        Arrays.fill(shuffleBuf.get(), ThreadLocalRandom.current().nextInt().toByte())
        shuffleBuf.get().shuffle()
    }

    @Setup(Level.Invocation)
    fun imitateComplexBuisnesLogic(blackhole: Blackhole) {
        when(cacheInvalidation) {
            CacheInvalidation.ALL_CORES -> {
                (1..Runtime.getRuntime().availableProcessors())
                    .map { CompletableFuture.runAsync(::prepare) }
                    .forEach { it.join() }
            }
            CacheInvalidation.SINGLE_CORE -> prepare()
            CacheInvalidation.NONE -> {}
        }
    }

    fun copyOf(other: ByteArray): ByteArray? {
        // Initialized with constant data, so nothing bad happens if we read other reference
        // We just wanted to screw the compiler.
        ref = other.copyOf(other.size)
        return ref
    }

    companion object {
        var ref: ByteArray? = null
    }
}