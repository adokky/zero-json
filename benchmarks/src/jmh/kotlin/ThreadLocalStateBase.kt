package dev.dokky.zerojson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
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
    private val shuffleBuf = ThreadLocal.withInitial { ByteArray(1 * 1024 * 1024) }

    var ktxJson: Json = Json { explicitNulls = false }
        private set
    var zJson: ZeroJson = ZeroJson(ktxJson.configuration, ktxJson.serializersModule)
        private set
    var serializer: KSerializer<Response<Person>> = serializer()
        private set

    private fun prepare() {
        Arrays.fill(shuffleBuf.get(), ThreadLocalRandom.current().nextInt().toByte())
        shuffleBuf.get().shuffle()
    }

    private var iteration = 0

    @Setup(Level.Trial)
    fun beforeTrial(blackhole: Blackhole) {
        iteration = 0
    }

    @Setup(Level.Iteration)
    fun imitateComplexBuisnesLogic(blackhole: Blackhole) {
        if (iteration++ < WARM_UP_ITERATIONS) return

        when (cacheInvalidation) {
            CacheInvalidation.NONE -> {}
            CacheInvalidation.SINGLE_CORE -> prepare()
            CacheInvalidation.ALL_CORES -> {
                (1..Runtime.getRuntime().availableProcessors())
                    .map { CompletableFuture.runAsync(::prepare) }
                    .forEach { it.join() }
            }
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