package com.github.phantomthief.pool;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;

import com.github.phantomthief.pool.impl.KeyAffinityBuilder;
import com.github.phantomthief.util.ThrowableConsumer;
import com.github.phantomthief.util.ThrowableFunction;

/**
 * @author w.vela
 * Created on 2018-02-03.
 */
public interface KeyAffinity<K, V> extends AutoCloseable, Iterable<V> {

    @Deprecated
    @Nonnull
    static <V> KeyAffinityBuilder<V> newBuilder() {
        return new KeyAffinityBuilder<>();
    }

    @Nonnull
    V select(K key);

    void finishCall(K key);

    @Deprecated
    default <T, X extends Throwable> T supply(K key, @Nonnull ThrowableFunction<V, T, X> func)
            throws X {
        checkNotNull(func);
        V one = select(key);
        try {
            return func.apply(one);
        } finally {
            finishCall(key);
        }
    }

    @Deprecated
    default <X extends Throwable> void run(K key, @Nonnull ThrowableConsumer<V, X> func) throws X {
        supply(key, it -> {
            func.accept(it);
            return null;
        });
    }
    
    boolean inited();
}
