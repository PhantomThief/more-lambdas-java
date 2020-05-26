package com.github.phantomthief.concurrent;

import static com.github.phantomthief.concurrent.MoreFutures.tryWait;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.base.Stopwatch;

/**
 * @author w.vela
 * Created on 2020-05-26.
 */
class MoreFuturesTest3 {

    @Test
    void test() {
        ExecutorService executor = newFixedThreadPool(20);
        List<Integer> keys = IntStream.range(1, 10).boxed().collect(toList());
        Stopwatch stopWatch = createStarted();
        Map<Integer, String> result = tryWait(keys, 5, SECONDS, it -> executor.submit(() -> {
            sleepUninterruptibly(1, SECONDS);
            return it + "";
        }));
        assertEquals(1, stopWatch.elapsed(SECONDS));
        for (Integer key : keys) {
            assertEquals(key + "", result.get(key));
        }
    }
}
