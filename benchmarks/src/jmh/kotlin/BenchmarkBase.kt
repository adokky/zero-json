package dev.dokky.zerojson

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

const val WARM_UP_ITERATIONS = 20_000

@Fork(1)
@Threads(1)

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 2000, batchSize = 1)
@Warmup(iterations = WARM_UP_ITERATIONS, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)

//@Measurement(iterations = 200, time = 300, timeUnit = TimeUnit.MILLISECONDS)
//@Warmup(iterations = 100, time = 100, timeUnit = TimeUnit.MILLISECONDS)
//@BenchmarkMode(Mode.Throughput)
abstract class BenchmarkBase {
}
