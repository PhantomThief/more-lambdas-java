package com.github.phantomthief.pool.impl;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.phantomthief.pool.KeyAffinity;

/**
 * @author w.vela
 * Created on 2018-02-09.
 */
class KeyAffinityTest {

    private KeyAffinity<Integer, String> keyAffinity;

    @BeforeEach
    void setUp() {
        keyAffinity = KeyAffinity.<String> newBuilder()
                .count(10)
                .factory(() -> "c:" + ThreadLocalRandom.current().nextInt(100))
                .build();
    }

    @Test
    void test() {
        ExecutorService executorService = newFixedThreadPool(50);
        Map<Integer, String> firstMapping = new ConcurrentHashMap<>();
        for (int i = 0; i < 20; i++) {
            int j = i;
            executorService.execute(() -> {
                keyAffinity.run(j, v -> {
                    firstMapping.put(j, v);
                    sleepUninterruptibly(10, SECONDS);
                });
            });
        }
        sleepUninterruptibly(1, SECONDS);
        for (int i = 0; i < 1000; i++) {
            executorService.execute(() -> {
                int key = ThreadLocalRandom.current().nextInt(20);
                keyAffinity.run(key, v -> {
                    String firstV = firstMapping.get(key);
                    assertEquals(firstV, v);
                });
            });
        }
        shutdownAndAwaitTermination(executorService, 1, DAYS);
    }

    @AfterEach
    void tearDown() throws Exception {
        keyAffinity.close();
    }
}
