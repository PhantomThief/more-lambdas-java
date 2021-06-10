package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.KeyAffinityExecutorBuilder.ALL_EXECUTORS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.immediateCancelledFuture;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private ConcurrentMap<K, SubstituentCallable<?>> substituentTaskMap;
    private boolean skipDuplicate = false;

    KeyAffinityExecutorImpl(Supplier<KeyAffinityImpl<K, ListeningExecutorService>> factory) {
        super(factory);
    }

    void setSkipDuplicate(boolean skipDuplicate) {
        this.skipDuplicate = skipDuplicate;
        if (skipDuplicate && substituentTaskMap == null) {
            substituentTaskMap = new ConcurrentHashMap<>();
        }
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

        if (skipDuplicate) {
            task = wrapSkipCheck(key, task);
            if (task == null) {
                return immediateCancelledFuture();
            }
        }

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

    /**
     * @return {@code null} if is not first added. for performances. only work on {{@link #skipDuplicate}} is {@code true}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private <T> Callable<T> wrapSkipCheck(K key, Callable<T> task) {
        boolean[] firstAdd = {false};
        SubstituentCallable result = substituentTaskMap.compute(key, (k, v) -> {
            if (v == null) {
                v = new SubstituentCallable<>(key, task);
                firstAdd[0] = true;
            } else { // 覆盖未执行的 task
                // callable 的赋值其实依赖 CHM.compute 内的锁实现，所以不要轻易修改 compute 内的赋值逻辑
                // 比如把 这个赋值 放到 compute 块的外部
                v.callable = (Callable) task;
            }
            return v;
        });
        if (firstAdd[0]) {
            return result;
        } else {
            return null;
        }
    }

    @Override
    public void executeEx(K key, @Nonnull ThrowableRunnable<Exception> task) {
        checkNotNull(task);

        ThrowableRunnable<Exception> finalTask;
        if (skipDuplicate) {
            Callable<Void> wrapCallable = wrapSkipCheck(key, () -> {
                task.run();
                return null;
            });
            if (wrapCallable == null) {
                return;
            } else {
                finalTask = wrapCallable::call;
            }
        } else {
            finalTask = task;
        }

        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        try {
            service.execute(() -> {
                try {
                    finalTask.run();
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

    private class SubstituentCallable<T> implements Callable<T> {

        private final K key;
        private Callable<T> callable;

        private SubstituentCallable(K key, Callable<T> callable) {
            this.key = key;
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            // 任务执行后，从 map 中移除
            // TODO 一个优化的方式，map 内维护正在运行的状态，这样就可以不依赖 KeyAffinityExecutor，使用普通的线程池也可以搞定了
            substituentTaskMap.remove(key);
            return callable.call();
        }
    }
}
