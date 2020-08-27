package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.KeyAffinityExecutorBuilder.ALL_EXECUTORS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.github.phantomthief.pool.KeyAffinity;
import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.pool.KeyAffinityExecutorStats;
import com.github.phantomthief.pool.KeyAffinityExecutorStats.SingleThreadPoolStats;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author w.vela
 * Created on 2018-11-30.
 */
class KeyAffinityExecutorImpl<K> extends LazyKeyAffinity<K, ListeningExecutorService> implements
                             KeyAffinityExecutor<K> {

    KeyAffinityExecutorImpl(Supplier<KeyAffinity<K, ListeningExecutorService>> factory) {
        super(factory);
    }

    @Override
    public void close() throws Exception {
        try {
            super.close();
        } finally {
            ALL_EXECUTORS.remove(this);
        }
    }

    @Nullable
    @Override
    public KeyAffinityExecutorStats stats() {
        List<SingleThreadPoolStats> list = new ArrayList<>();
        for (ListeningExecutorService executor : this) {
            if (executor instanceof ThreadListeningExecutorService) {
                ThreadListeningExecutorService t1 = (ThreadListeningExecutorService) executor;
                list.add(new SingleThreadPoolStats(t1.getMaximumPoolSize(), t1.getActiveCount(),
                        t1.getQueueSize(), t1.getQueueRemainingCapacity()));
            } else {
                throw new IllegalStateException("cannot get stats for " + this);
            }
        }
        return new KeyAffinityExecutorStats(list);
    }
}
