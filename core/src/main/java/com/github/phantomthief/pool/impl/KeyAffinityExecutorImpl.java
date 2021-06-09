package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.KeyAffinityExecutorBuilder.ALL_EXECUTORS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.pool.KeyAffinityExecutorStats;
import com.github.phantomthief.pool.KeyAffinityExecutorStats.SingleThreadPoolStats;
import com.github.phantomthief.tuple.Tuple;
import com.github.phantomthief.tuple.TwoTuple;
import com.github.phantomthief.util.ThrowableRunnable;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * @author w.vela
 * Created on 2018-11-30.
 */
class KeyAffinityExecutorImpl<K> extends LazyKeyAffinity<K, ListeningExecutorService> implements
        KeyAffinityExecutor<K> {

    private final Map<K, SubstituentTask> substituentTaskMap = new ConcurrentHashMap<>();
    private boolean skipDuplicate = false;

    KeyAffinityExecutorImpl(Supplier<KeyAffinityImpl<K, ListeningExecutorService>> factory) {
        super(factory);
    }

    public void setSkipDuplicate(boolean skipDuplicate) {
        this.skipDuplicate = skipDuplicate;
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


    private <T> TwoTuple<Callable<T>, Boolean> wrapCallable(K key, @Nonnull Callable<T> task) {
        if (skipDuplicate) {
            // 如果首次添加成功，提交任务到线程池
            AtomicBoolean isFirstAdd = new AtomicBoolean();
            SubstituentTask substituentTask = substituentTaskMap.compute(key, (k, v) -> {
                if (v == null) {
                    v = new SubstituentCallable(key, task);
                    isFirstAdd.set(true);
                } else { // 覆盖未执行的 task
                    v.replace(task);
                }
                return v;
            });

            if (!(substituentTask instanceof Callable)) {
                throw new IllegalStateException("found illegal task type. key:[" + key + "]");
            }

            Callable<T> addTask = (Callable<T>) substituentTask;

            return Tuple.tuple(addTask, isFirstAdd.get());

        }
        return Tuple.tuple(task, true);
    }

    @Override
    public <T> ListenableFuture<T> submit(K key, @Nonnull Callable<T> task) {
        checkNotNull(task);

        TwoTuple<Callable<T>, Boolean> wrapTuple = wrapCallable(key, task);
        if (!wrapTuple.getSecond()) {
            return ListenableFutureTask.create(() -> null);
        }

        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        try {
            ListenableFuture<T> future = service.submit(wrapTuple.getFirst());
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

    private TwoTuple<ThrowableRunnable<Exception>, Boolean> wrapRunnable(K key,
            @Nonnull ThrowableRunnable<Exception> task) {
        if (skipDuplicate) {
            // 如果首次添加成功，提交任务到线程池
            AtomicBoolean isFirstAdd = new AtomicBoolean();
            SubstituentTask substituentTask = substituentTaskMap.compute(key, (k, v) -> {
                if (v == null) {
                    v = new SubstituentRunnable(key, task);
                    isFirstAdd.set(true);
                } else { // 覆盖未执行的 task
                    v.replace(task);
                }
                return v;
            });

            if (!(substituentTask instanceof ThrowableRunnable)) {
                throw new IllegalStateException("found illegal task type. key:[" + key + "]");
            }

            ThrowableRunnable<Exception> addTask = (ThrowableRunnable<Exception>) substituentTask;
            return Tuple.tuple(addTask, isFirstAdd.get());

        }
        return Tuple.tuple(task, true);
    }

    @Override
    public void executeEx(K key, @Nonnull ThrowableRunnable<Exception> task) {
        checkNotNull(task);
        TwoTuple<ThrowableRunnable<Exception>, Boolean> wrapTuple = wrapRunnable(key, task);
        if (!wrapTuple.getSecond()) {
            return;
        }

        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        try {
            service.execute(() -> {
                try {
                    wrapTuple.getFirst().run();
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

    private interface SubstituentTask<T> {

        void replace(T t);

    }

    private class SubstituentRunnable implements SubstituentTask<ThrowableRunnable<Exception>>, ThrowableRunnable<Exception> {

        private final K key;
        private ThrowableRunnable<Exception> runnable;

        public SubstituentRunnable(K key, ThrowableRunnable<Exception> runnable) {
            this.key = key;
            this.runnable = runnable;
        }

        @Override
        public void replace(ThrowableRunnable<Exception> runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() throws Exception {
            substituentTaskMap.remove(key);
            runnable.run();
        }
    }

    private class SubstituentCallable<T> implements SubstituentTask<Callable<T>>, Callable<T> {

        private final K key;
        private Callable<T> callable;

        public SubstituentCallable(K key, Callable<T> callable) {
            this.key = key;
            this.callable = callable;
        }

        @Override
        public void replace(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            // 任务执行后，从 map 中移除
            substituentTaskMap.remove(key);
            return callable.call();
        }

    }

}
