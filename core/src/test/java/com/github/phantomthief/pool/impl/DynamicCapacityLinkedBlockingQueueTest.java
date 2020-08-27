package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.DynamicCapacityLinkedBlockingQueue.lazyDynamicCapacityLinkedBlockingQueue;
import static com.google.common.util.concurrent.SimpleTimeLimiter.create;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.TimeLimiter;

/**
 * @author w.vela
 * Created on 2020-08-20.
 */
class DynamicCapacityLinkedBlockingQueueTest {
    private static final Logger logger = LoggerFactory.getLogger(DynamicCapacityLinkedBlockingQueueTest.class);

    @Test
    void test() throws InterruptedException {
        int[] count = {0};
        boolean[] access = {false};
        BlockingQueue<Object> queue = lazyDynamicCapacityLinkedBlockingQueue(() -> {
            access[0] = true;
            return count[0];
        });
        assertFalse(access[0]);
        queue.put("test");
        assertTrue(access[0]);
        assertEquals("test", queue.poll());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());

        count[0] = 10;
        sleepUninterruptibly(1, SECONDS);

        for (int i = 0; i < 10; i++) {
            assertTrue(queue.offer("test"));
        }
        assertFalse(queue.offer("test"));
        assertEquals(0, queue.remainingCapacity());

        count[0] = 20;
        sleepUninterruptibly(1, SECONDS);

        for (int i = 0; i < 10; i++) {
            assertTrue(queue.offer("test"));
        }
        assertFalse(queue.offer("test"));

        count[0] = 5;
        sleepUninterruptibly(1, SECONDS);

        assertFalse(queue.offer("test"));

        for (int i = 0; i < 15; i++) {
            assertEquals("test", queue.poll());
        }

        assertFalse(queue.offer("test"));
        assertEquals("test", queue.poll());
        assertTrue(queue.offer("test"));
    }

    @Test
    void testBasic() throws InterruptedException {
        int[] count = {0};
        BlockingQueue<Object> queue = lazyDynamicCapacityLinkedBlockingQueue(() -> count[0]);
        queue.add("test");
        // may lead NPE on sometimes.
        assertEquals("test", queue.remove());

        queue.add("test");
        assertTrue(queue.contains("test"));
        assertEquals("test", queue.take());
        queue.offer("test", 1, SECONDS);
        assertEquals("test", queue.peek());
        assertEquals("test", queue.element());
        assertEquals("test", queue.poll(1, SECONDS));

        queue.addAll(ImmutableList.of("test"));
        assertTrue(queue.containsAll(ImmutableList.of("test")));
        assertFalse(queue.retainAll(ImmutableList.of("test")));
        for (Object o : queue) {
            assertEquals("test", o);
        }
        assertTrue(queue.removeAll(ImmutableList.of("test")));

        queue.add("test");
        queue.clear();
        assertEquals(0, queue.size());

        queue.add("test");
        queue.drainTo(new ArrayList<>());
        queue.drainTo(new ArrayList<>(), 1);
        assertThrows(NoSuchElementException.class, () -> queue.remove());

        queue.toString();

        Object[] objects = queue.toArray();
        assertTrue(objects.length == 0);
        queue.add("test");
        Object[] strings = queue.toArray(new Object[0]);
        queue.remove("test");
        logger.info("{}", queue.toString());

        queue.clear();
        assertTrue(queue.isEmpty());
        queue.add("test");
        queue.remove(); // 这里会导致 NPE，感觉不是太健壮啊

        queue.add("test");
        queue.stream().forEach(it -> assertEquals("test", it));
        queue.forEach(it -> assertEquals("test", it));
        queue.removeIf(it -> it.equals("test"));
        assertTrue(queue.isEmpty());
    }

    @Test
    void testPutBlock() throws InterruptedException {
        int[] count = {10};
        DynamicCapacityLinkedBlockingQueue<Object> queue = new DynamicCapacityLinkedBlockingQueue<>(() -> count[0]);
        for (int i = 0; i < 10; i++) {
            assertTrue(queue.offer("test"));
        }
        TimeLimiter timeLimiter = create(newFixedThreadPool(10));
        assertThrows(TimeoutException.class, () -> {
            timeLimiter.runWithTimeout(() -> {
                try {
                    queue.put("test");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, 1, SECONDS);
        });
        count[0] = 20;
        sleepUninterruptibly(1, SECONDS);
        queue.put("test");
    }
}