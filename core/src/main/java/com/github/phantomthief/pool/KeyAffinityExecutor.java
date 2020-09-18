package com.github.phantomthief.pool;

import static com.github.phantomthief.pool.KeyAffinityExecutorUtils.RANDOM_THRESHOLD;
import static com.github.phantomthief.pool.KeyAffinityExecutorUtils.executor;
import static com.github.phantomthief.util.MoreReflection.logDeprecated;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.phantomthief.pool.impl.KeyAffinityExecutorBuilder;
import com.github.phantomthief.util.ThrowableRunnable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @param <K> 该泛型如果是自定义类型，一定要实现正确的 {@link Object#hashCode()} 和 {@link Object#equals(Object)}
 *
 * @author w.vela
 * Created on 2018-02-09.
 */
public interface KeyAffinityExecutor<K> extends KeyAffinity<K, ListeningExecutorService> {

    int DEFAULT_QUEUE_SIZE = 100;

    /**
     * 一般推荐使用其它几个 {@link #newSerializingExecutor} 重载版本，只有当需要定制具体参数时，才用本方法；
     *
     * 一个典型的使用方法是:
     * <pre> {@code
     * class MyClass {
     *   private final KeyAffinityExecutor<Integer> keyExecutor = newKeyAffinityExecutor()
     *                       .parallelism(10) // 设置并发度
     *                       .executor(() -> new ExecutorWithStats(newSingleThreadExecutor())) // 工场方法
     *                       .usingRandom(true) // 强制采用随机方式分配
     *                       .build();
     *   void foo(User user) {
     *     Future<Integer> fansCount = keyExecutor.submit(user.getUserId(), () -> {
     *       return fansService.getByUserId(user.getUserId());
     *     });
     *   }
     * }
     * }</pre>
     */
    @Nonnull
    static KeyAffinityExecutorBuilder newKeyAffinityExecutor() {
        return new KeyAffinityExecutorBuilder();
    }

    @Nonnull
    static <K> KeyAffinityExecutor<K> newSerializingExecutor(int parallelism, String threadName) {
        return newSerializingExecutor(parallelism, DEFAULT_QUEUE_SIZE, threadName);
    }

    /**
     * @param parallelism max concurrency for task submitted.
     * @param queueBufferSize max queue size for every executor, 0 means unbounded queue(DANGEROUS).
     * @param threadName see {@link ThreadFactoryBuilder#setNameFormat(String)}
     */
    @Nonnull
    static <K> KeyAffinityExecutor<K> newSerializingExecutor(int parallelism, int queueBufferSize,
            String threadName) {
        return newKeyAffinityExecutor()
                .parallelism(parallelism)
                .executor(executor(threadName, queueBufferSize))
                .build();
    }

    /**
     * 本版本是 {@link #newSerializingExecutor(int, int, String)} 的 动态版本，可以动态设置并发度
     *
     * @param parallelism max concurrency for task submitted.
     * @param queueBufferSize max queue size for every executor, 0 means unbounded queue(DANGEROUS).
     * @param threadName see {@link ThreadFactoryBuilder#setNameFormat(String)}
     */
    @Nonnull
    static <K> KeyAffinityExecutor<K> newSerializingExecutor(IntSupplier parallelism, IntSupplier queueBufferSize,
            String threadName) {
        return newKeyAffinityExecutor()
                .parallelism(parallelism)
                .executor(executor(threadName, queueBufferSize))
                .usingRandom(it -> it > RANDOM_THRESHOLD)
                .build();
    }

    <T> ListenableFuture<T> submit(K key, @Nonnull Callable<T> task);

    /**
     * use {@link #executeEx} instead
     */
    @Deprecated
    default ListenableFuture<?> execute(K key, @Nonnull Runnable task) {
        checkNotNull(task);

        logDeprecated("Deprecated calling:KeyAffinityExecutor.execute() at ({}), use executeEx() instead.");

        return submit(key, () -> {
            task.run();
            return null;
        });
    }

    void executeEx(K key, @Nonnull ThrowableRunnable<Exception> task);

    /**
     * @return {@code} null if not inited
     * @throws IllegalStateException if cannot calc stats
     */
    @Nullable
    KeyAffinityExecutorStats stats();

    /**
     * for stats only, cannot do any operations.
     */
    static Collection<KeyAffinityExecutor<?>> allExecutorsForStats() {
        return KeyAffinityExecutorBuilder.getAllExecutors();
    }
}
