package com.github.phantomthief.pool;

import javax.annotation.Nonnull;

import com.github.phantomthief.pool.impl.KeyAffinityBuilder;

/**
 * @author w.vela
 * Created on 2018-02-03.
 */
@Deprecated
public interface KeyAffinity<K, V> extends AutoCloseable, Iterable<V> {

    @Deprecated
    @Nonnull
    static <V> KeyAffinityBuilder<V> newBuilder() {
        return new KeyAffinityBuilder<>();
    }

    @Deprecated
    @Nonnull
    V select(K key);

    @Deprecated
    void finishCall(K key);

    boolean inited();
}
