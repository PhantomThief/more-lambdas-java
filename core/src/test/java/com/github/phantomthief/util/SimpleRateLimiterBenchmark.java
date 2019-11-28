package com.github.phantomthief.util;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Benchmark                           Mode  Cnt         Score         Error  Units
 * SimpleRateLimiterBenchmark.guava   thrpt    5  10786111.210 ±  648463.363  ops/s
 * SimpleRateLimiterBenchmark.simple  thrpt    5  36536015.255 ± 1020655.069  ops/s
 *
 * @author w.vela
 * Created on 2019-11-28.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 5, time = 1)
@Threads(8)
@Fork(1)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class SimpleRateLimiterBenchmark {

    private static final RateLimiter GUAVA = RateLimiter.create(1.0D);
    private static final SimpleRateLimiter SIMPLE = SimpleRateLimiter.create(1.0D);

    @Benchmark
    public static void guava() {
        GUAVA.tryAcquire();
    }

    @Benchmark
    public static void simple() {
        SIMPLE.tryAcquire();
    }
}
