package com.github.phantomthief.pool.impl;

import static com.google.common.base.Preconditions.checkNotNull;
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

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.phantomthief.pool.KeyAffinity;
import com.github.phantomthief.util.ThrowableConsumer;
import com.github.phantomthief.util.ThrowableFunction;

/**
 * @author w.vela
 * Created on 2018-02-09.
 */
class KeyAffinityTest {

    private KeyAffinity<Integer, String> keyAffinity;

    @BeforeEach
    void setUp() {
        keyAffinity = new KeyAffinityBuilder<String>()
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
                run(keyAffinity, j, v -> {
                    firstMapping.put(j, v);
                    sleepUninterruptibly(10, SECONDS);
                });
            });
        }
        sleepUninterruptibly(1, SECONDS);
        for (int i = 0; i < 1000; i++) {
            executorService.execute(() -> {
                int key = ThreadLocalRandom.current().nextInt(20);
                run(keyAffinity, key, v -> {
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

    private static <T, X extends Throwable, K, V> T supply(KeyAffinity<K, V> keyAffinity, K key,
            @Nonnull ThrowableFunction<V, T, X> func) throws X {
        checkNotNull(func);
        V one = keyAffinity.select(key);
        try {
            return func.apply(one);
        } finally {
            keyAffinity.finishCall(key);
        }
    }

    private static <X extends Throwable, K, V> void run(KeyAffinity<K, V> keyAffinity, K key,
            @Nonnull ThrowableConsumer<V, X> func) throws X {
        supply(keyAffinity,key, it -> {
            func.accept(it);
            return null;
        });
    }
}
