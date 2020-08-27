package com.github.phantomthief.pool.impl;

import java.util.Iterator;

import javax.annotation.Nonnull;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.pool.KeyAffinityExecutorStats;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author w.vela
 * Created on 2018-11-29.
 */
class KeyAffinityExecutorForStats<K> implements KeyAffinityExecutor<K> {

    private final KeyAffinityExecutor<K> delegate;

    private KeyAffinityExecutorForStats(KeyAffinityExecutor<K> delegate) {
        this.delegate = delegate;
    }

    static <K> KeyAffinityExecutor<K> wrapStats(KeyAffinityExecutor<K> executor) {
        if (executor instanceof KeyAffinityExecutorForStats) {
            return executor;
        } else {
            return new KeyAffinityExecutorForStats<>(executor);
        }
    }

    @Nonnull
    @Override
    public ListeningExecutorService select(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void finishCall(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean inited() {
        return delegate.inited();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<ListeningExecutorService> iterator() {
        return delegate.iterator();
    }

    @Override
    public KeyAffinityExecutorStats stats() {
        if (delegate.inited()) {
            return delegate.stats();
        } else {
            return null;
        }
    }
}
