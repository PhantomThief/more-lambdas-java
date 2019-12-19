package com.github.phantomthief.test;

import static com.github.phantomthief.util.MoreFunctions.catching;
import static com.github.phantomthief.util.MoreFunctions.consumerKv;
import static com.github.phantomthief.util.MoreFunctions.filterKv;
import static com.github.phantomthief.util.MoreFunctions.mapKv;
import static com.github.phantomthief.util.MoreFunctions.runParallel;
import static com.github.phantomthief.util.MoreFunctions.runWithThreadName;
import static com.github.phantomthief.util.MoreFunctions.throwing;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

/**
 * @author w.vela
 */
class MoreFunctionsTest {

    @Test
    void testTrying() {
        assertNull(catching(i -> function(i, Exception::new), 1));
        assertNull(catching(i -> function(i, IllegalArgumentException::new), 1));
        assertEquals("1", catching(i -> function(i, null), 1));
    }

    @Test
    void testThrowing() {
        assertThrows(IllegalArgumentException.class, () -> throwing(() -> {
            throw new IllegalArgumentException();
        }));
        assertTrue(assertThrows(RuntimeException.class, () -> throwing(() -> {
            throw new IOException();
        })).getCause() instanceof IOException);
    }

    private <X extends Throwable> String function(int i, Supplier<X> exception) throws X {
        if (exception != null) {
            X x = exception.get();
            throw x;
        } else {
            return i + "";
        }
    }

    @Test
    void testParallel() {
        List<Integer> list = Stream.iterate(1, i -> i + 1).limit(10000).collect(toList());
        runParallel(new ForkJoinPool(10), () -> list.stream().parallel()
                .forEach(System.out::println));
    }

    @Test
    void testThreadName() {
        String mySuffix = "MySuffix";
        runWithThreadName(it -> it + mySuffix, () -> {
            assertTrue(Thread.currentThread().getName().endsWith(mySuffix));
        });
    }

    @Test
    void testKv() {
        Map<String, Integer> map = ImmutableMap.of("test", 1, "a1", 2);
        map.entrySet().stream()
                .filter(filterKv((k, v) -> true))
                .forEach(consumerKv((k, v) -> System.out.println(k + "==>" + v)));
        map.entrySet().stream()
                .map(mapKv((k, v) -> v))
                .forEach(System.out::println);
    }
}
