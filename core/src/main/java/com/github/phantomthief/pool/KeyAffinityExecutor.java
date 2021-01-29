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
 * 按指定的Key亲和顺序消费的执行器
 * <p>KeyAffinityExecutor是一个特殊的任务执行器{@link java.util.concurrent.Executor}，
 * 它可以确保投递进来的任务按Key相同的任务依照提交顺序依次执行。在既要通过并行处理来提高吞吐量、又要保证一定范围内的
 * 任务按照严格的先后顺序来运行的场景下非常适用。</p>
 * <p>KeyAffinityExecutor的内建实现方式，是将指定的Key映射到固定的单线程执行器上，它内部会维护
 * 多个（数量可配）这样的单线程执行器，来保持一定的任务并行度。</p>
 * <p>需要注意的是，此接口定义的KeyAffinityExecutor，并不要求Key相同的任务在相同的线程上运行，
 * 尽管实现类可以按照这种方式来实现，但它并非一个强制性的要求，因此在使用时也请不要依赖这样的假定。</p>
 * <p>
 * 一个典型的使用方式是:
 * <pre>{@code
 * class MyClass {
 *   private final KeyAffinityExecutor<Integer> keyExecutor = newSerializingExecutor(10, "user-fans-count-%d");
 *   void foo(User user) {
 *     Future<Integer> fansCount = keyExecutor.submit(user.getUserId(), () -> {
 *       return fansService.getByUserId(user.getUserId());
 *     });
 *   }
 * }
 * }</pre>
 *
 * @param <K> 该泛型如果是自定义类型，一定要实现正确的 {@link Object#hashCode()}
 * @author w.vela
 * Created on 2018-02-09.
 */
public interface KeyAffinityExecutor<K> extends KeyAffinity<K, ListeningExecutorService> {

    int DEFAULT_QUEUE_SIZE = 100;

    /**
     * 创建{@link KeyAffinityExecutorBuilder}构造器来构造KeyAffinityExecutor的实现对象
     * <p>
     * 一般推荐使用其它几个 {@link #newSerializingExecutor} 重载版本，只有当需要定制具体参数时，才用本方法；
     * <p>
     *
     * @see #newSerializingExecutor
     */
    @Nonnull
    static KeyAffinityExecutorBuilder newKeyAffinityExecutor() {
        return new KeyAffinityExecutorBuilder();
    }

    /**
     * 创建一个{@link KeyAffinityExecutor}对象
     *
     * @param parallelism 指定{@link KeyAffinityExecutor}并发度，即最多并行执行的任务数
     * @param threadName 执行线程的名称，支持使用%d占位符来指定线程序号，参考{@link ThreadFactoryBuilder#setNameFormat(String)}
     * @return 返回{@link KeyAffinityExecutor}对象
     */
    @Nonnull
    static <K> KeyAffinityExecutor<K> newSerializingExecutor(int parallelism, String threadName) {
        return newSerializingExecutor(parallelism, DEFAULT_QUEUE_SIZE, threadName);
    }

    /**
     * 创建一个{@link KeyAffinityExecutor}对象
     *
     * @param parallelism 指定{@link KeyAffinityExecutor}并发度，即最多并行执行的任务数
     * @param queueBufferSize 任务队列的长度，请根据任务的吞吐量设置合适的大小，0为无上限（OOM警告）
     * @param threadName 执行线程的名称，支持使用%d占位符来指定线程序号，参考{@link ThreadFactoryBuilder#setNameFormat(String)}
     * @return 返回{@link KeyAffinityExecutor}对象
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
     * 创建一个{@link KeyAffinityExecutor}对象
     * <p>此方法是{@link #newSerializingExecutor(int, int, String)} 的动态版本，可以动态设置并发度</p>
     *
     * @param parallelism 指定{@link KeyAffinityExecutor}并发度，即最多并行执行的任务数
     * @param queueBufferSize 任务队列的长度，请根据任务的吞吐量设置合适的大小，0为无上限（OOM警告）
     * @param threadName 执行线程的名称，支持使用%d占位符来指定线程序号，参考{@link ThreadFactoryBuilder#setNameFormat(String)}
     * @return 返回{@link KeyAffinityExecutor}对象
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

    /**
     * 提交执行一个任务
     *
     * @param key 任务对应的Key，此对象务必实现hashCode、equals，以确保可以起到标识作用
     * @param task 任务执行对象
     * @return 携带任务执行返回值的 {@link ListenableFuture}
     */
    <T> ListenableFuture<T> submit(K key, @Nonnull Callable<T> task);

    /**
     * 已废弃：提交执行一个任务
     * 请使用 {@link #executeEx} 替代它
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

    /**
     * 提交执行一个任务
     *
     * @param key 任务对应的Key，此对象务必实现hashCode、equals，以确保可以起到标识作用
     * @param task 任务执行对象
     */
    void executeEx(K key, @Nonnull ThrowableRunnable<Exception> task);

    /**
     * 获取当前{@link KeyAffinityExecutor}的统计对象，以获取统计信息
     *
     * @return 返回统计对象，在当前{@link KeyAffinityExecutor}初始化之前返回null
     * @throws IllegalStateException 获取统计对象失败时抛出此异常
     */
    @Nullable
    KeyAffinityExecutorStats stats();

    /**
     * 获取当前所有的{@link KeyAffinityExecutor}对象集合，用于统计信息获取
     */
    static Collection<KeyAffinityExecutor<?>> allExecutorsForStats() {
        return KeyAffinityExecutorBuilder.getAllExecutors();
    }
}
