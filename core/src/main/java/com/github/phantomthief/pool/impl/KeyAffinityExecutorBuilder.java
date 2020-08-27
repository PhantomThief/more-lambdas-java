package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.KeyAffinityExecutorForStats.wrapStats;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.TimeUnit.DAYS;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.util.SimpleRateLimiter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author w.vela
 * Created on 2018-02-09.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class KeyAffinityExecutorBuilder {

    static final Map<KeyAffinityExecutor<?>, KeyAffinityExecutor<?>> ALL_EXECUTORS = new ConcurrentHashMap<>();
    private final KeyAffinityBuilder<ListeningExecutorService> builder = new KeyAffinityBuilder<>();

    private boolean usingDynamic = false;
    private boolean shutdownAfterClose = true;

    @Nonnull
    public <K> KeyAffinityExecutor<K> build() {
        if (usingDynamic && !shutdownAfterClose) {
            throw new IllegalStateException("cannot close shutdown after close when enable dynamic count.");
        }
        if (shutdownAfterClose) {
            builder.depose(it -> shutdownAndAwaitTermination(it, 1, DAYS));
        }
        builder.ensure();
        KeyAffinityExecutorImpl<K> result = new KeyAffinityExecutorImpl<>(builder::buildInner);
        ALL_EXECUTORS.put(result, wrapStats(result));
        return result;
    }

    /**
     * @param value default value if {@code true}
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder shutdownExecutorAfterClose(boolean value) {
        shutdownAfterClose = value;
        return this;
    }

    /**
     * see {@link KeyAffinityBuilder#usingRandom(boolean)}
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder usingRandom(boolean value) {
        builder.usingRandom(value);
        return this;
    }

    /**
     * see {@link KeyAffinityBuilder#usingRandom(boolean)}
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder usingRandom(IntPredicate value) {
        builder.usingRandom(value);
        return this;
    }

    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder executor(@Nonnull Supplier<ExecutorService> factory) {
        checkNotNull(factory);
        builder.factory(() -> {
            ExecutorService executor = factory.get();
            if (executor instanceof ListeningExecutorService) {
                return (ListeningExecutorService) executor;
            } else if (executor instanceof ThreadPoolExecutor) {
                return new ThreadListeningExecutorService((ThreadPoolExecutor) executor);
            } else {
                return listeningDecorator(executor);
            }
        });
        return this;
    }

    /**
     * 建议使用 {@link #parallelism(int)}，语义更清晰
     */
    @Deprecated
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder count(int count) {
        return parallelism(count);
    }

    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder parallelism(int value) {
        builder.count(value);
        return this;
    }

    @CheckReturnValue
    @Nonnull
    @VisibleForTesting
    KeyAffinityExecutorBuilder counterChecker(BooleanSupplier value) {
        builder.counterChecker(value);
        return this;
    }

    /**
     * 建议使用 {@link #parallelism(IntSupplier)}，语义更清晰
     */
    @Deprecated
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder count(IntSupplier count) {
        return parallelism(count);
    }

    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder parallelism(IntSupplier count) {
        builder.count(count);
        usingDynamic = true;
        SimpleRateLimiter rateLimiter = SimpleRateLimiter.create(1);
        builder.counterChecker(rateLimiter::tryAcquire);
        return this;
    }


    public static Collection<KeyAffinityExecutor<?>> getAllExecutors() {
        return unmodifiableCollection(ALL_EXECUTORS.values());
    }

    @VisibleForTesting
    static void clearAllExecutors() {
        ALL_EXECUTORS.clear();
    }
}
