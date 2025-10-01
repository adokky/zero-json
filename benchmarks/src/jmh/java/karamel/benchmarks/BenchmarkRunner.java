package karamel.benchmarks;

import kotlin.collections.ArraysKt;
import kotlinx.serialization.json.Json;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Fork(1)
public class BenchmarkRunner {
    @State(Scope.Thread)
    public static class ThreadLocalState {
        private final byte[] buffer = new byte[128 * 1024 * 1024];

        public Json ktxJson = BenchmarksKt.createKotlinxJson();

        public ThreadLocalState() {
            Arrays.fill(buffer, (byte) ThreadLocalRandom.current().nextInt());
        }

        @Setup(Level.Invocation)
        public void prepareForIteration(Blackhole blackhole) {
            // imitate complex business logic:
            // invalidate CPU caches by shuffling large array
            ArraysKt.shuffle(buffer);
            System.gc();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 10, time = 1)
    @Threads(2)
    @Measurement(iterations = 20, batchSize = 1, time = 10, timeUnit = TimeUnit.MILLISECONDS)
    public Object b1_kotlinx_json(ThreadLocalState state) {
        return BenchmarksKt.ktxBenchmark(state);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 10, time = 1)
    @Threads(2)
    @Measurement(iterations = 20, batchSize = 1, time = 10, timeUnit = TimeUnit.MILLISECONDS)
    public Object b1_kotlinx_json_no_copy(ThreadLocalState state) {
        return BenchmarksKt.ktxBenchmarkNoCopy(state);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 10, time = 1)
    @Threads(2)
    @Measurement(iterations = 20, batchSize = 1, time = 10, timeUnit = TimeUnit.MILLISECONDS)
    public Object b2_json5(ThreadLocalState state) {
        return BenchmarksKt.json5Benchmark(state);
    }

    public static void main(String... args) throws RunnerException {
        /*
        -bm <mode>benchmarkMode
        -bs <int>batchSize
        -e <regexp+>exclude
        -f <int> fork
        -foe <bool> failOnError
        -gc <bool> forceGC
        -i <int> iterations
        -jvm <string> jvm
        -jvmArgs <string> jvmArgs
        -jvmArgsAppend <string>jvmArgsAppend
        -jvmArgsPrepend <string> jvmArgsPrepend
        -o <filename> humanOutputFile
        -opi <int> operationsPerInvocation
        -p <param={v,}*> benchmarkParameters?
        -prof <profiler> profilers
        -r <time> timeOnIteration
        -rf <type> resultFormat
        -rff <filename> resultsFile
        -si <bool> synchronizeIterations
        -t <int> threads
        -tg <int+> threadGroups
        -to <time> jmhTimeout
        -tu <TU> timeUnit
        -v <mode> verbosity
        -w <time> warmup
        -wbs <int> warmupBatchSize
        -wf <int> warmupForks
        -wi <int> warmupIterations
        -wm <mode> warmupMode
        -wmb <regexp+> warmupBenchmarks
         */
        Options opt = new OptionsBuilder()
                .include(BenchmarkRunner.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();

//        Main.main(new String[] {
////                "-lprof",
////                "-prof", "perf"
////                "-prof", "perfasm"
////                "-prof", "hs_comp",
////                "-prof", "hs_gc"
//            "-prof", "gc"
//        });
    }
}
