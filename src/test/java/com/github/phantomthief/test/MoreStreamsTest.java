package com.github.phantomthief.test;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.MoreStreams;

/**
 * @author w.vela
 */
class MoreStreamsTest {

    @Test
    void testRange() {
        MoreStreams.longRangeClosed(10, 15).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.longRangeClosed(15, 10).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.intRangeClosed(10, 15).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.intRangeClosed(15, 10).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.intRangeClosed(15, 15).forEach(System.out::println);
    }

    @Test
    void testPartition() {
        Stream<Integer> stream = Stream.iterate(1, i -> i + 1);
        MoreStreams.partition(stream, 100).limit(10).forEach(System.out::println);
    }
}
