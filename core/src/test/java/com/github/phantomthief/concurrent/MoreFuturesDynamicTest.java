package com.github.phantomthief.concurrent;

import static com.github.phantomthief.concurrent.MoreFutures.scheduleWithDynamicDelay;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author w.vela
 * Created on 2019-11-26.
 */
class MoreFuturesDynamicTest {

    private static final Logger logger = LoggerFactory.getLogger(MoreFuturesDynamicTest.class);

    @Disabled
    @Test
    void test() {
        ScheduledExecutorService scheduled = newSingleThreadScheduledExecutor();
        int[] i = {1};
        int[] run = {0};
        Future<?> future = scheduleWithDynamicDelay(scheduled, () -> {
            Duration duration = ofSeconds(i[0]);
            i[0]++;
            return duration;
        }, () -> {
            logger.info("run...");
            run[0]++;
        });
        sleepUninterruptibly(7, SECONDS);
        assertEquals(3, run[0]);
        future.cancel(false);
        sleepUninterruptibly(5, SECONDS);
        assertEquals(3, run[0]);
    }
}
