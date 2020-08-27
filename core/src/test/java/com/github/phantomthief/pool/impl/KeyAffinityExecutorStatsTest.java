package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.KeyAffinityExecutor.DEFAULT_QUEUE_SIZE;
import static com.github.phantomthief.pool.KeyAffinityExecutor.allExecutorsForStats;
import static com.github.phantomthief.pool.KeyAffinityExecutor.newSerializingExecutor;
import static com.github.phantomthief.pool.impl.KeyAffinityExecutorBuilder.clearAllExecutors;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.pool.KeyAffinityExecutorStats;
import com.github.phantomthief.pool.KeyAffinityExecutorStats.SingleThreadPoolStats;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author w.vela
 * Created on 2018-11-29.
 */
class KeyAffinityExecutorStatsTest {

    private static final Logger logger = LoggerFactory
            .getLogger(KeyAffinityExecutorStatsTest.class);

    @Test
    void test() throws Exception {
        clearAllExecutors();
        KeyAffinityExecutor<Integer> executor1 = newSerializingExecutor(10, "test");
        KeyAffinityExecutor<Integer> executor2 = newSerializingExecutor(10, "test-2");
        Collection<KeyAffinityExecutor<?>> all = allExecutorsForStats();
        assertEquals(2, all.size());
        for (KeyAffinityExecutor<?> keyAffinityExecutor : all) {
            assertFalse(keyAffinityExecutor.inited());
        }
        executor1.executeEx(1, () -> {});
        executor2.executeEx(1, () -> {});
        for (KeyAffinityExecutor<?> keyAffinityExecutor : all) {
            assertTrue(keyAffinityExecutor.inited());
            List<ListeningExecutorService> exeList = new ArrayList<>();
            for (ListeningExecutorService executorService : keyAffinityExecutor) {
                exeList.add(executorService);
            }
            assertEquals(10, exeList.size());
            KeyAffinityExecutorStats stats = keyAffinityExecutor.stats();
            assertNotNull(stats);
            logger.info("stats:{}", stats);
            assertEquals(10, stats.getThreadPoolStats().size());
            for (SingleThreadPoolStats threadPoolStat : stats.getThreadPoolStats()) {
                assertEquals(0, threadPoolStat.getActiveThreadCount());
                assertEquals(1, threadPoolStat.getParallelism());
            }

            assertThrows(UnsupportedOperationException.class,
                    () -> keyAffinityExecutor.select(null));
            assertThrows(UnsupportedOperationException.class,
                    () -> keyAffinityExecutor.finishCall(null));
        }

        executor1.executeEx(1, () -> sleepUninterruptibly(2, SECONDS));
        executor2.executeEx(1, () -> sleepUninterruptibly(2, SECONDS));
        for (KeyAffinityExecutor<?> keyAffinityExecutor : all) {
            KeyAffinityExecutorStats stats = keyAffinityExecutor.stats();
            assertNotNull(stats);
            logger.info("stats:{}", stats);
            assertEquals(10, stats.getThreadPoolStats().size());
            assertEquals(1, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getActiveThreadCount)
                    .sum());
            assertEquals(0, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getQueueSize)
                    .sum());
            assertEquals(10, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getParallelism)
                    .sum());
            assertEquals(DEFAULT_QUEUE_SIZE * 10, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getQueueRemainingCapacity)
                    .sum());
        }
        sleepUninterruptibly(2, SECONDS);

        executor1.executeEx(1, () -> sleepUninterruptibly(2, SECONDS));
        executor2.executeEx(1, () -> sleepUninterruptibly(2, SECONDS));
        executor1.executeEx(1, () -> sleepUninterruptibly(2, SECONDS));
        executor2.executeEx(1, () -> sleepUninterruptibly(2, SECONDS));

        for (KeyAffinityExecutor<?> keyAffinityExecutor : all) {
            KeyAffinityExecutorStats stats = keyAffinityExecutor.stats();
            assertNotNull(stats);
            logger.info("stats:{}", stats);
            assertEquals(10, stats.getThreadPoolStats().size());
            assertEquals(1, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getActiveThreadCount)
                    .sum());
            assertEquals(1, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getQueueSize)
                    .sum());
            assertEquals(10, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getParallelism)
                    .sum());
            assertEquals(DEFAULT_QUEUE_SIZE * 10 - 1, stats.getThreadPoolStats().stream()
                    .mapToInt(SingleThreadPoolStats::getQueueRemainingCapacity)
                    .sum());
        }

        executor1.close();
        executor2.close();
        assertTrue(allExecutorsForStats().isEmpty());
    }
}
