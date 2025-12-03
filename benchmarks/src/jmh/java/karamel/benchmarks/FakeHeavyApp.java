package karamel.benchmarks;

import dev.dokky.zerojson.ZeroJson;
import io.kodec.buffers.ArrayBuffer;
import io.kodec.buffers.MutableBuffer;
import kotlin.collections.ArraysKt;
import kotlinx.serialization.json.Json;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Fork(1)
@Threads(1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 1000, batchSize = 1)
@Warmup(iterations = 400, batchSize = 1)
@BenchmarkMode({Mode.SingleShotTime})
public class FakeHeavyApp {
    @State(Scope.Thread)
    public static class TLS implements ThreadLocalState {
        private final byte[] shuffleBuf = new byte[6 * 1024 * 1024];
        static final int BUF_SIZE = 10_000;

        public Json ktxJson = BenchmarksKt.createKotlinxJson();
        public ZeroJson zJson = ZeroJson.create(ktxJson.getConfiguration(), ktxJson.getSerializersModule());
        private ByteArrayOutputStream baos;
        private ArrayBuffer outputBuf = new ArrayBuffer(new byte[BUF_SIZE], 0, BUF_SIZE);

        public TLS() {
            Arrays.fill(shuffleBuf, (byte) ThreadLocalRandom.current().nextInt());
        }

        @Setup(Level.Invocation)
        public void prepareForIteration(Blackhole blackhole) {
            baos = new ByteArrayOutputStream(BUF_SIZE);
            outputBuf.clear(0, BUF_SIZE);
            // imitate complex business logic:
            // invalidate CPU caches by shuffling large array
            ArraysKt.shuffle(shuffleBuf);
        }

        @Override
        public Json getKtxJson() { return ktxJson; }

        @Override
        public ZeroJson getZJson() { return zJson; }

        private static volatile byte[] ref;
        @Override
        public byte[] copyOf(byte[] other) {
            // Initialized with constant data, so nothing bad happens if we read other reference
            // We just wanted to screw the compiler.
            ref = Arrays.copyOf(other, other.length);
            return ref;
        }

        @Override
        public OutputStream getOutputStream() { return baos; }

        @Override
        public MutableBuffer getBuffer() { return outputBuf; }
    }

    @Benchmark
    public Object decode_bytes_kotlinx(TLS state) {
        return BenchmarksKt.decodeBytesKtx(state);
    }

    @Benchmark
    public Object decode_bytes_kotlinx_no_copy(TLS state) {
        return BenchmarksKt.decodeBytesKtxNoCopy(state);
    }

    @Benchmark
    public Object decode_string_kotlinx(TLS state) {
        return BenchmarksKt.decodeStringKtx(state);
    }

    @Benchmark
    public Object decode_tree_kotlinx(TLS state) {
        return BenchmarksKt.decodeTreeKtx(state);
    }

    @Benchmark
    public Object encode_bytes_kotlinx(TLS state) {
        return BenchmarksKt.encodeBytesKtx(state);
    }

    @Benchmark
    public Object encode_bytes_kotlinx_no_copy(TLS state) {
        return BenchmarksKt.encodeBytesKtxNoCopy(state);
    }

    @Benchmark
    public Object encode_string_kotlinx(TLS state) {
        return BenchmarksKt.encodeStringKtx(state);
    }

    @Benchmark
    public Object encode_tree_kotlinx(TLS state) {
        return BenchmarksKt.encodeTreeKtx(state);
    }

    @Benchmark
    public Object decode_bytes_zjson(TLS state) {
        return BenchmarksKt.decodeBytesZeroJson(state);
    }

    @Benchmark
    public Object encode_bytes_zjson(TLS state) {
        return BenchmarksKt.encodeBytesZeroJson(state);
    }

    @Benchmark
    public Object encode_bytes_zjson_no_copy(TLS state) {
        return BenchmarksKt.encodeBytesZeroJsonNoCopy(state);
    }

    @Benchmark
    public Object encode_string_zjson(TLS state) {
        return BenchmarksKt.encodeStringZeroJson(state);
    }

    @Benchmark
    public Object encode_tree_zjson(TLS state) {
        return BenchmarksKt.encodeTreeZeroJson(state);
    }

//    @Benchmark
    public Object decode_bytes_zjson_thread_local(TLS state) {
        return BenchmarksKt.decodeBytesZeroJsonThreadLocal(state);
    }

//    @Benchmark
    public Object decode_bytes_zjson_two_level(TLS state) {
        return BenchmarksKt.decodeBytesZeroJsonTwoLevel(state);
    }

    @Benchmark
    public Object decode_string_zjson(TLS state) {
        return BenchmarksKt.decodeStringZeroJson(state);
    }

    @Benchmark
    public Object decode_tree_zjson(TLS state) {
        return BenchmarksKt.decodeTreeZeroJson(state);
    }
}

