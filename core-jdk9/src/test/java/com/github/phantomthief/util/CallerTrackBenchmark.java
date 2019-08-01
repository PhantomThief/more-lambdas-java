package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreReflection.getCallerPlace;

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

/**
 * 性能指标：
 * <p>
 * Benchmark                       Mode  Cnt       Score        Error  Units
 * CallerTrackBenchmark.testJdk8  thrpt    5   59081.301 ±  33148.476  ops/s
 * CallerTrackBenchmark.testJdk9  thrpt    5  329034.388 ± 443312.075  ops/s
 *
 * @author w.vela
 * Created on 2019-08-01.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 5, time = 1)
@Threads(8)
@Fork(1)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class CallerTrackBenchmark {

    @Benchmark
    public static void testJdk8() {
        StackTraceElement callerPlace = MyTest.test3();
        callerPlace = MyTest2.test41();
    }

    @Benchmark
    public static void testJdk9() {
        StackTraceElement callerPlace = MyTest.test39();
        callerPlace = MyTest2.test42();
    }

    static class MyTest {

        private static StackTraceProviderJdk9 jdk9 = new StackTraceProviderJdk9();
        private static StackTraceProviderJdk8 jdk8 = new StackTraceProviderJdk8();

        @Deprecated
        static void test() {
            System.err.println(getCallerPlace());
        }

        @Deprecated
        static void test2() {
            test();
        }

        @Deprecated
        static StackTraceElement test3() {
            return jdk8.getCallerPlace(StackTraceProviderJdk8.class);
        }

        @Deprecated
        static StackTraceElement test4() {
            return jdk8.getCallerPlace(MoreReflectionTest.MyTest.class);
        }

        @Deprecated
        static StackTraceElement test39() {
            return jdk9.getCallerPlace(StackTraceProviderJdk9.class);
        }

        @Deprecated
        static StackTraceElement test49() {
            return jdk9.getCallerPlace(MoreReflectionTest.MyTest.class);
        }
    }

    static class MyTest2 {
        static StackTraceElement test41() {
            return MyTest.test4();
        }

        static StackTraceElement test42() {
            return MyTest.test49();
        }
    }
}
