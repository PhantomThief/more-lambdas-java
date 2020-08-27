package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.KeyAffinityExecutor.newSerializingExecutor;
import static org.openjdk.jmh.annotations.Mode.Throughput;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import com.github.phantomthief.pool.KeyAffinityExecutor;

/**
 * Benchmark                                  Mode  Cnt        Score        Error  Units
 * KeyAffinityExecutorBenchmark.test         thrpt    5  2344169.134 ± 186206.161  ops/s
 * KeyAffinityExecutorBenchmark.testDynamic  thrpt    5  2478918.267 ± 250481.212  ops/s
 * KeyAffinityExecutorBenchmark.testJdk      thrpt    5  7526776.023 ± 278178.966  ops/s
 * KeyAffinityExecutorBenchmark.testRandom   thrpt    5  3869465.719 ± 131909.710  ops/s
 *
 * @author w.vela
 * Created on 2020-07-02.
 */
@BenchmarkMode(Throughput)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 5, time = 1)
@Threads(8)
@Fork(1)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class KeyAffinityExecutorBenchmark {

    private final KeyAffinityExecutor<Integer> executor1 = newSerializingExecutor(10, "test");
    private final KeyAffinityExecutor<Integer> executor2 = newSerializingExecutor(30, "test2");
    private final Executor executor3 = Executors.newFixedThreadPool(10);
    private final KeyAffinityExecutor<Integer> executor4 = newSerializingExecutor(() -> 30, () -> 100, "test3");

    @Benchmark
    public void test() {
        executor1.executeEx(ThreadLocalRandom.current().nextInt(100), () -> {});
    }

    @Benchmark
    public void testRandom() {
        executor2.executeEx(ThreadLocalRandom.current().nextInt(100), () -> {});
    }

    @Benchmark
    public void testJdk() {
        executor3.execute(() -> {});
    }

    @Benchmark
    public void testDynamic() {
        executor4.executeEx(ThreadLocalRandom.current().nextInt(100), () -> {});
    }
}
