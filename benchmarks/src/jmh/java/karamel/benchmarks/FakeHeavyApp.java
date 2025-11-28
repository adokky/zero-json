package karamel.benchmarks;

import dev.dokky.zerojson.ZeroJson;
import dev.dokky.zerojson.ZeroJsonConfiguration;
import kotlin.collections.ArraysKt;
import kotlinx.serialization.json.Json;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Fork(1)
@Threads(4)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 1000, batchSize = 1)
@Warmup(iterations = 100, time = 1)
@BenchmarkMode(Mode.SingleShotTime)
public class FakeHeavyApp {
    @State(Scope.Thread)
    public static class TLS implements ThreadLocalState {
        private final byte[] buffer = new byte[8 * 1024 * 1024];

        public Json ktxJson = BenchmarksKt.createKotlinxJson();
        public ZeroJson zJson = ZeroJson.create(ZeroJsonConfiguration.Default);

        public TLS() {
            Arrays.fill(buffer, (byte) ThreadLocalRandom.current().nextInt());
        }

        @Setup(Level.Invocation)
        public void prepareForIteration(Blackhole blackhole) {
            // imitate complex business logic:
            // invalidate CPU caches by shuffling large array
            ArraysKt.shuffle(buffer);
        }

        @Override
        public Json getKtxJson() { return ktxJson; }

        @Override
        public ZeroJson getZJson() { return zJson; }

        private static volatile byte[] ref;
        @Override
        public byte[] copyOf(byte[] other) {
            // Initialized with constant data, so nothing bad happens if we read foreign other reference
            // We just wanted to screw the compiler.
            ref = Arrays.copyOf(other, other.length);
            return ref;
        }
    }

    @Benchmark
    public Object b1_kotlinx_json(TLS state) {
        return BenchmarksKt.ktxBenchmark(state);
    }

    @Benchmark
    public Object b1_kotlinx_json_no_copy(TLS state) {
        return BenchmarksKt.ktxBenchmarkNoCopy(state);
    }

    @Benchmark
    public Object b2_json5(TLS state) {
        return BenchmarksKt.json5Benchmark(state);
    }
}