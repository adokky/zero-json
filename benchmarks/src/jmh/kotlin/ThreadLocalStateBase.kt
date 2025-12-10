package dev.dokky.zerojson

import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.infra.Blackhole
import java.util.*
import java.util.concurrent.ThreadLocalRandom

abstract class ThreadLocalStateBase {
    private val shuffleBuf = ByteArray(6 * 1024 * 1024)

    var ktxJson: Json = Json {
        explicitNulls = false
    }
    var zJson: ZeroJson = ZeroJson(ktxJson.configuration, ktxJson.serializersModule)

    init {
        Arrays.fill(shuffleBuf, ThreadLocalRandom.current().nextInt().toByte())
    }

    @Setup(Level.Invocation)
    fun imitateComplexBuisnesLogic(blackhole: Blackhole) {
        // imitate complex business logic:
        // invalidate CPU caches by shuffling large array
        shuffleBuf.shuffle()
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