package dev.dokky.zerojson

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Fork(1)
@Threads(1)

//@OutputTimeUnit(TimeUnit.MICROSECONDS)
//@Measurement(iterations = 2000, batchSize = 1)
//@Warmup(iterations = 1200, batchSize = 1)
//@BenchmarkMode(Mode.SingleShotTime)

@Measurement(iterations = 200, time = 300, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 60, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
abstract class BenchmarkBase {
}
