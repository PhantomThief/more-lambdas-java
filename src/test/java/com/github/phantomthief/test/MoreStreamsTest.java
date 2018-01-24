package com.github.phantomthief.test;

import static com.github.phantomthief.util.MoreStreams.intRangeClosed;
import static com.github.phantomthief.util.MoreStreams.longRangeClosed;
import static com.github.phantomthief.util.MoreStreams.partition;
import static com.github.phantomthief.util.MoreStreams.supplyParallel;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 */
class MoreStreamsTest {

    @Test
    void testRange() {
        longRangeClosed(10, 15).forEach(System.out::println);
        System.out.println("===");
        longRangeClosed(15, 10).forEach(System.out::println);
        System.out.println("===");
        intRangeClosed(10, 15).forEach(System.out::println);
        System.out.println("===");
        intRangeClosed(15, 10).forEach(System.out::println);
        System.out.println("===");
        intRangeClosed(15, 15).forEach(System.out::println);
    }

    @Test
    void testPartition() {
        Stream<Integer> stream = Stream.iterate(1, i -> i + 1);
        partition(stream, 100).limit(10).forEach(System.out::println);
    }

    @Test
    void testParallel() {
        Stream<Integer> stream = Stream.iterate(1, i -> i + 1);
        List<Integer> integers = supplyParallel(new ForkJoinPool(10000), stream,
                it -> it.limit(1000).collect(toList()));
        System.out.println(integers);
    }
}
