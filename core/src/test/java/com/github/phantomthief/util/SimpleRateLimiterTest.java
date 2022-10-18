package com.github.phantomthief.util;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2019-11-28.
 */
class SimpleRateLimiterTest {

    @Test
    void test() {
        SimpleRateLimiter limiter = SimpleRateLimiter.create(1.0D);
        int j = 0;
        for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) {
                sleepUninterruptibly(1, SECONDS);
            }
            if (limiter.tryAcquire()) {
                j++;
            }
        }
        assertEquals(10, j);
        assertEquals(90, limiter.getSkipCountAndClear());
        assertEquals(0, limiter.getSkipCountAndClear());
    }

    @Test
    void test2() {
        SimpleRateLimiter limiter = SimpleRateLimiter.create(2.0D);
        int j = 0;
        for (int i = 0; i < 50; i++) {
            if (i % 5 == 0) {
                sleepUninterruptibly(500, MILLISECONDS);
            }
            if (limiter.tryAcquire()) {
                j++;
            }
        }
        assertEquals(10, j);
    }

    @Test
    void test3() {
        SimpleRateLimiter limiter = SimpleRateLimiter.create(0.5);
        int j = 0;
        for (int i = 0; i < 80; i++) {
            if (i % 10 == 0) {
                sleepUninterruptibly(1, SECONDS);
            }
            if (limiter.tryAcquire()) {
                j++;
            }
        }
        assertEquals(4, j);
        assertEquals(76, limiter.getSkipCountAndClear());
        assertEquals(0, limiter.getSkipCountAndClear());
    }

    @Test
    void test4() {
        double permitsPerSecond = 1.0 / 3600.0; // report at most 1 log in an hour
        SimpleRateLimiter limiter = SimpleRateLimiter.create(permitsPerSecond);
        assertTrue(limiter.tryAcquire());
        assertFalse(limiter.tryAcquire());
        assertEquals(HOURS.toNanos(1), limiter.getAllowTimesPerNanos());
    }

    @Test
    void test5() {
        SimpleRateLimiter limiter = SimpleRateLimiter.create(1.0D);
        new Thread(() -> {
            sleepUninterruptibly(4, SECONDS);
            limiter.setRate(0.5);
        }).start();
        int j = 0;
        for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) {
                sleepUninterruptibly(1, SECONDS);
            }
            if (limiter.tryAcquire()) {
                j++;
            }
        }
        assertEquals(7, j);  // 4 + 6/2
        assertEquals(93, limiter.getSkipCountAndClear());
        assertEquals(0, limiter.getSkipCountAndClear());
    }

    @Test
    void test6() {
        SimpleRateLimiter limiter = SimpleRateLimiter.create(1.0D);
        new Thread(() -> {
            sleepUninterruptibly(4, SECONDS);
            limiter.setPeriod(Duration.ofSeconds(2));
        }).start();
        int j = 0;
        for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) {
                sleepUninterruptibly(1, SECONDS);
            }
            if (limiter.tryAcquire()) {
                j++;
            }
        }
        assertEquals(7, j);  // 4 + 6/2
        assertEquals(93, limiter.getSkipCountAndClear());
        assertEquals(0, limiter.getSkipCountAndClear());
    }
}