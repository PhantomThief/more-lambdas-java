package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

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

    private final LongAdder skip = new LongAdder();

    /**
     * 多少ns允许一次请求
     */
    private long allowTimesPerNanos;
    private volatile long lastAcquiredNanos;

    private SimpleRateLimiter(long allowTimesPerNanos) {
        checkState(allowTimesPerNanos > 0);
        this.allowTimesPerNanos = allowTimesPerNanos;
    }

    /**
     * 创建一个{@link SimpleRateLimiter}对象，限制每次的时间间隔
     *
     * @param periodPerTimes 时间间隔
     * @return {@link SimpleRateLimiter}对象
     */
    public static SimpleRateLimiter createByPeriod(Duration periodPerTimes) {
        return new SimpleRateLimiter(checkNotNull(periodPerTimes).toNanos());
    }

    /**
     * 创建一个{@link SimpleRateLimiter}对象，限制每秒允许的请求次数
     *
     * @param permitsPerSecond 请求次数
     * @return {@link SimpleRateLimiter}对象
     */
    public static SimpleRateLimiter create(double permitsPerSecond) {
        checkState(permitsPerSecond > 0);
        long allowTimesPerNanos = (long) (SECONDS.toNanos(1) / permitsPerSecond);
        return new SimpleRateLimiter(allowTimesPerNanos);
    }

    /**
     * 设置当前{@link SimpleRateLimiter}对象每秒允许的请求次数
     *
     * @param permitsPerSecond 每秒请求次数
     */
    public void setRate(double permitsPerSecond) {
        checkState(permitsPerSecond > 0);
        long thisPeriod = (long) (SECONDS.toNanos(1) / permitsPerSecond);
        checkState(thisPeriod > 0);
        this.allowTimesPerNanos = thisPeriod;
        this.lastAcquiredNanos = 0;
    }

    /**
     * 设置当前{@link SimpleRateLimiter}对象每次请求的间隔时间
     *
     * @param periodPerTimes 间隔时间
     */
    public void setPeriod(Duration periodPerTimes) {
        long thisPeriod = checkNotNull(periodPerTimes).toNanos();
        checkState(thisPeriod > 0);
        this.allowTimesPerNanos = thisPeriod;
        this.lastAcquiredNanos = 0;
    }

    long getAllowTimesPerNanos() {
        return allowTimesPerNanos;
    }

    /**
     * 判断本次请求是否获得准许处理请求
     *
     * @return 是否获得准许
     */
    public boolean tryAcquire() {
        long nanoTime = System.nanoTime();
        if (nanoTime >= lastAcquiredNanos + allowTimesPerNanos || lastAcquiredNanos == 0) {
            synchronized (this) {
                if (nanoTime >= lastAcquiredNanos + allowTimesPerNanos || lastAcquiredNanos == 0) {
                    lastAcquiredNanos = nanoTime;
                    return true;
                }
            }
        }
        skip.increment();
        return false;
    }

    /**
     * 计算到上次执行本操作前，共计多少个没有获取到准许，计算完成后重新计数
     *
     * @return 累计跳过的请求数
     */
    public long getSkipCountAndClear() {
        return skip.sumThenReset();
    }
}
