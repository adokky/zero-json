package dev.dokky.zerojson

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Fork(1)
@Threads(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 1000, batchSize = 1)
@Warmup(iterations = 400, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
abstract class BenchmarkBase {
}