package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.KeyAffinityExecutor.newSerializingExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.phantomthief.pool.KeyAffinityExecutor;

/**
 * @author duomn10
 * Created on 2021/6/9
 */
public class KeyAffinityExecutorSkipDuplicateTest {

    @Test
    void testSkipDuplicate() throws Exception {
        Map<Integer, AtomicInteger> countMap = new ConcurrentHashMap<>();
        KeyAffinityExecutor<Integer> keyExecutor = newSerializingExecutor(2, 20, true, "test");
        for (int i = 0; i < 20; i++) {
            int key = i % 2;
            keyExecutor.executeEx(key, () -> {
                if (key == 0) {
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                countMap.compute(key, (k, v) -> {
                    if (v == null) {
                        v = new AtomicInteger();
                    }
                    v.incrementAndGet();
                    return v;
                });
            });
            TimeUnit.MILLISECONDS.sleep(10);
        }
        keyExecutor.close();
        Assertions.assertEquals(10, countMap.get(1).get());
        Assertions.assertTrue(10 > countMap.get(0).get());
    }
}
