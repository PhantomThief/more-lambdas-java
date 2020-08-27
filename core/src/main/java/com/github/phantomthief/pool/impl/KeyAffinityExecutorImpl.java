package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.KeyAffinityExecutorBuilder.ALL_EXECUTORS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.pool.KeyAffinityExecutorStats;
import com.github.phantomthief.pool.KeyAffinityExecutorStats.SingleThreadPoolStats;
import com.github.phantomthief.util.ThrowableRunnable;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * @author w.vela
 * Created on 2018-11-30.
 */
class KeyAffinityExecutorImpl<K> extends LazyKeyAffinity<K, ListeningExecutorService> implements
                             KeyAffinityExecutor<K> {

    KeyAffinityExecutorImpl(Supplier<KeyAffinityImpl<K, ListeningExecutorService>> factory) {
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

    @Override
    public <T> ListenableFuture<T> submit(K key, @Nonnull Callable<T> task) {
        checkNotNull(task);

        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        try {
            ListenableFuture<T> future = service.submit(task);
            addCallback(future, new FutureCallback<Object>() {

                @Override
                public void onSuccess(@Nullable Object result) {
                    finishCall(key);
                }

                @Override
                public void onFailure(Throwable t) {
                    finishCall(key);
                }
            }, directExecutor());
            addCallback = true;
            return future;
        } finally {
            if (!addCallback) {
                finishCall(key);
            }
        }
    }

    @Override
    public void executeEx(K key, @Nonnull ThrowableRunnable<Exception> task) {
        checkNotNull(task);

        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        try {
            service.execute(() -> {
                try {
                    task.run();
                } catch (Throwable e) { // pass to uncaught exception handler
                    throwIfUnchecked(e);
                    throw new UncheckedExecutionException(e);
                } finally {
                    finishCall(key);
                }
            });
            addCallback = true;
        } finally {
            if (!addCallback) {
                finishCall(key);
            }
        }
    }
}
