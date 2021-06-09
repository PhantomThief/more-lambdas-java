package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.KeyAffinityExecutor.newSerializingExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.util.ThrowableRunnable;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author duomn10
 * Created on 2021/6/9
 */
class KeyAffinityExecutorSkipDuplicateTest {

    private static final Logger logger = LoggerFactory.getLogger(KeyAffinityExecutorSkipDuplicateTest.class);

    @Test
    void testSkipDuplicate() throws Exception {
        Map<Integer, Integer> countMap = new ConcurrentHashMap<>();
        int skipCount = 0;
        KeyAffinityExecutor<Integer> keyExecutor = newSerializingExecutor(2, 20, true, "test");
        for (int i = 0; i < 20; i++) {
            int key = i % 2;
            ListenableFuture<Object> future = keyExecutor.submit(key, () -> {
                if (key == 0) {
                    MILLISECONDS.sleep(100);
                }
                countMap.merge(key, 1, Math::addExact);
                return null;
            });
            if (future.isCancelled()) {
                assertThrows(CancellationException.class, future::get);
                skipCount++;
            }
            MILLISECONDS.sleep(10);
        }
        keyExecutor.close();
        assertEquals(10, countMap.get(1));
        assertTrue(10 > countMap.get(0));
        assertEquals(skipCount, 10 - countMap.get(0));
        logger.info("execute count:{}", countMap);
    }

    @Test
    void testSkipDuplicateWithMultipleMethod() throws Exception {
        Map<Integer, Integer> countMap = new ConcurrentHashMap<>();
        KeyAffinityExecutor<Integer> keyExecutor = newSerializingExecutor(2, 20, true, "test");
        for (int i = 0; i < 20; i++) {
            int key = i % 2;
            ThrowableRunnable<Exception> runnable = () -> {
                if (key == 0) {
                    MILLISECONDS.sleep(100);
                }
                countMap.merge(key, 1, Math::addExact);
            };
            if (i % 3 == 0) {
                keyExecutor.executeEx(key, runnable);
            } else {
                keyExecutor.submit(key, () -> {
                    runnable.run();
                    return null;
                });
            }
            MILLISECONDS.sleep(10);
        }
        keyExecutor.close();
        assertEquals(10, countMap.get(1));
        assertTrue(10 > countMap.get(0));
        logger.info("execute count:{}", countMap);
    }
}
