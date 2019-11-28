package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;

import javax.annotation.concurrent.ThreadSafe;

/**
 * 对于低频场景（例如n秒一次），使用 {@link com.google.common.util.concurrent.RateLimiter} 开销较大
 * 本实现基于上一次访问时间戳模式，大幅降低开销
 *
 * @author w.vela
 * Created on 2019-11-28.
 */
@ThreadSafe
public class SimpleRateLimiter {

    /**
     * 多少ns允许一次请求
     */
    private long allowTimesPerNanos;
    private volatile long lastAcquiredNanos;

    private SimpleRateLimiter(long allowTimesPerNanos) {
        checkState(allowTimesPerNanos > 0);
        this.allowTimesPerNanos = allowTimesPerNanos;
    }

    public static SimpleRateLimiter createByPeriod(Duration periodPerTimes) {
        return new SimpleRateLimiter(checkNotNull(periodPerTimes).toNanos());
    }

    public static SimpleRateLimiter create(double permitsPerSecond) {
        checkState(permitsPerSecond > 0);
        long allowTimesPerNanos = (long) (SECONDS.toNanos(1) / permitsPerSecond);
        return new SimpleRateLimiter(allowTimesPerNanos);
    }

    public void setRate(double permitsPerSecond) {
        checkState(permitsPerSecond > 0);
        long thisPeriod = (long) (SECONDS.toNanos(1) / permitsPerSecond);
        checkState(thisPeriod > 0);
        this.allowTimesPerNanos = thisPeriod;
    }

    public void setPeriod(Duration periodPerTimes) {
        long thisPeriod = checkNotNull(periodPerTimes).toNanos();
        checkState(thisPeriod > 0);
        this.allowTimesPerNanos = thisPeriod;
    }

    public boolean tryAcquire() {
        long nanoTime = System.nanoTime();
        if (nanoTime >= lastAcquiredNanos + allowTimesPerNanos) {
            synchronized (this) {
                if (nanoTime >= lastAcquiredNanos + allowTimesPerNanos) {
                    lastAcquiredNanos = nanoTime;
                    return true;
                }
            }
        }
        return false;
    }
}
