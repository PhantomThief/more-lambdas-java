package com.github.phantomthief.concurrent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import com.github.phantomthief.util.ThrowableFunction;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;

/**
 * @author w.vela
 * Created on 2018-06-25.
 */
public class MoreFutures {

    /**
     * @throws UncheckedTimeoutException if timeout occurred.
     * @throws java.util.concurrent.CancellationException if task was canceled.
     * @throws ExecutionError if a {@link Error} occurred.
     * @throws UncheckedExecutionException if a normal Exception occurred.
     */
    public static <T> T getUnchecked(@Nonnull Future<? extends T> future,
            @Nonnull Duration duration) {
        checkNotNull(duration);
        return getUnchecked(future, duration.toNanos(), NANOSECONDS);
    }

    /**
     * @throws UncheckedTimeoutException if timeout occurred.
     * @throws java.util.concurrent.CancellationException if task was canceled.
     * @throws ExecutionError if a {@link Error} occurred.
     * @throws UncheckedExecutionException if a normal Exception occurred.
     */
    public static <T> T getUnchecked(@Nonnull Future<? extends T> future, @Nonnegative long timeout,
            @Nonnull TimeUnit unit) {
        checkArgument(timeout > 0);
        checkNotNull(future);
        try {
            return getUninterruptibly(future, timeout, unit);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw new ExecutionError((Error) cause);
            } else {
                throw new UncheckedExecutionException(cause);
            }
        } catch (TimeoutException e) {
            throw new UncheckedTimeoutException(e);
        }
    }

    @Nonnull
    public static <K extends Future<V>, V> TryWaitResult<K, V> tryWait(@Nonnull Iterable<K> futures,
            @Nonnull Duration duration) {
        return tryWait(futures, duration.toNanos(), NANOSECONDS);
    }

    @Nonnull
    public static <K extends Future<V>, V> TryWaitResult<K, V> tryWait(@Nonnull Iterable<K> futures,
            @Nonnegative long timeout, @Nonnull TimeUnit unit) {
        return tryWait(futures, timeout, unit, it -> it);
    }

    @Nonnull
    public static <K, V, X extends Throwable> TryWaitResult<K, V> tryWait(@Nonnull Iterable<K> keys,
            @Nonnull Duration duration, @Nonnull ThrowableFunction<K, Future<V>, X> asyncFunc)
            throws X {
        checkNotNull(duration);
        return tryWait(keys, duration.toNanos(), NANOSECONDS, asyncFunc);
    }

    @Nonnull
    public static <K, V, X extends Throwable> TryWaitResult<K, V> tryWait(@Nonnull Iterable<K> keys,
            @Nonnegative long timeout, @Nonnull TimeUnit unit,
            @Nonnull ThrowableFunction<K, Future<V>, X> asyncFunc) throws X {
        checkArgument(timeout > 0);
        checkNotNull(unit);
        checkNotNull(keys);
        checkNotNull(asyncFunc);

        Map<Future<? extends V>, V> successMap = new LinkedHashMap<>();
        Map<Future<? extends V>, Throwable> failMap = new LinkedHashMap<>();
        Map<Future<? extends V>, TimeoutException> timeoutMap = new LinkedHashMap<>();
        Map<Future<? extends V>, CancellationException> cancelMap = new LinkedHashMap<>();

        long remainingNanos = unit.toNanos(timeout);
        long end = nanoTime() + remainingNanos;

        Map<Future<? extends V>, K> futureKeyMap = new IdentityHashMap<>();
        for (K key : keys) {
            checkNotNull(key);
            Future<V> future = asyncFunc.apply(key);
            checkNotNull(future);
            futureKeyMap.put(future, key);
            if (remainingNanos <= 0) {
                waitAndCollect(successMap, failMap, timeoutMap, cancelMap, future, 1L);
                continue;
            }
            waitAndCollect(successMap, failMap, timeoutMap, cancelMap, future, remainingNanos);
            remainingNanos = end - nanoTime();
        }
        return new TryWaitResult<>(successMap, failMap, timeoutMap, cancelMap, futureKeyMap);
    }

    private static <T> void waitAndCollect(Map<Future<? extends T>, T> successMap,
            Map<Future<? extends T>, Throwable> failMap,
            Map<Future<? extends T>, TimeoutException> timeoutMap,
            Map<Future<? extends T>, CancellationException> cancelMap, Future<? extends T> future,
            long thisWait) {
        try {
            T t = getUninterruptibly(future, thisWait, NANOSECONDS);
            successMap.put(future, t);
        } catch (CancellationException e) {
            cancelMap.put(future, e);
        } catch (TimeoutException e) {
            timeoutMap.put(future, e);
        } catch (ExecutionException e) {
            failMap.put(future, e.getCause());
        } catch (Throwable e) {
            failMap.put(future, e);
        }
    }
}
