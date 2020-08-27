package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.KeyAffinityExecutorUtils.RANDOM_THRESHOLD;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.github.phantomthief.pool.KeyAffinity;
import com.github.phantomthief.util.ThrowableConsumer;
import com.google.common.annotations.VisibleForTesting;

/**
 * @author w.vela
 * Created on 2018-02-09.
 */
@NotThreadSafe
public class KeyAffinityBuilder<V> {

    private Supplier<V> factory;
    private IntSupplier count;
    private ThrowableConsumer<V, Exception> depose;
    private IntPredicate usingRandom;
    private BooleanSupplier counterChecker;

    public <K> KeyAffinity<K, V> build() {
        ensure();
        return new LazyKeyAffinity<>(this::buildInner);
    }

    <K> KeyAffinity<K, V> buildInner() {
        return new KeyAffinityImpl<>(factory, count, depose, usingRandom, counterChecker);
    }

    void ensure() {
        if (count == null || count.getAsInt() <= 0) {
            throw new IllegalArgumentException("no count found.");
        }
        if (counterChecker == null) {
            counterChecker = () -> true;
        }
        if (depose == null) {
            depose = it -> { };
        }
        if (usingRandom == null) {
            usingRandom = it -> it > RANDOM_THRESHOLD;
        }
    }

    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    @VisibleForTesting
    <T extends KeyAffinityBuilder<V>> T counterChecker(@Nonnull BooleanSupplier value) {
        this.counterChecker = checkNotNull(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    public <T extends KeyAffinityBuilder<V>> T factory(@Nonnull Supplier<V> value) {
        this.factory = checkNotNull(value);
        return (T) this;
    }

    /**
     * whether to use random strategy or less concurrency strategy
     * @param value {@code true} is random strategy and {@code false} is less concurrency strategy
     * default value is {@code true} is {@link #count} larger than {@link com.github.phantomthief.pool.KeyAffinityExecutorUtils#RANDOM_THRESHOLD}
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    public <T extends KeyAffinityBuilder<V>> T usingRandom(boolean value) {
        return usingRandom(it -> value);
    }

    /**
     * whether to use random strategy or less concurrency strategy
     * @param value {@code true} is random strategy and {@code false} is less concurrency strategy
     * default value is {@code true} is {@link #count} larger than {@link com.github.phantomthief.pool.KeyAffinityExecutorUtils#RANDOM_THRESHOLD}
     */
    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    public <T extends KeyAffinityBuilder<V>> T usingRandom(@Nonnull IntPredicate value) {
        this.usingRandom = checkNotNull(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    public <T extends KeyAffinityBuilder<V>> T count(@Nonnegative int value) {
        checkArgument(value > 0);
        this.count = () -> value;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    public <T extends KeyAffinityBuilder<V>> T count(@Nonnull IntSupplier value) {
        this.count = checkNotNull(value);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @CheckReturnValue
    @Nonnull
    public <T extends KeyAffinityBuilder<V>> T depose(@Nonnegative ThrowableConsumer<V, Exception> value) {
        this.depose = checkNotNull(value);
        return (T) this;
    }
}
