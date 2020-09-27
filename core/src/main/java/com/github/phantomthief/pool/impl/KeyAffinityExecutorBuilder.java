package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.pool.impl.KeyAffinityExecutorForStats.wrapStats;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.TimeUnit.DAYS;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import com.github.phantomthief.pool.KeyAffinityExecutor;
import com.github.phantomthief.util.SimpleRateLimiter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * {@link KeyAffinityExecutor}构造器
 * <p>用于创建{@link KeyAffinityExecutor}的实现类的实例</p>
 *
 * @author w.vela
 * Created on 2018-02-09.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class KeyAffinityExecutorBuilder {

    static final Map<KeyAffinityExecutor<?>, KeyAffinityExecutor<?>> ALL_EXECUTORS = new ConcurrentHashMap<>();
    private final KeyAffinityBuilder<ListeningExecutorService> builder = new KeyAffinityBuilder<>();

    private boolean usingDynamic = false;
    private boolean shutdownAfterClose = true;

    /**
     * 创建{@link KeyAffinityExecutor}对象
     *
     * @return 使用构造器配置创建的{@link KeyAffinityExecutor}对象
     */
    @Nonnull
    public <K> KeyAffinityExecutor<K> build() {
        if (usingDynamic && !shutdownAfterClose) {
            throw new IllegalStateException("cannot close shutdown after close when enable dynamic count.");
        }
        if (shutdownAfterClose) {
            builder.depose(it -> shutdownAndAwaitTermination(it, 1, DAYS));
        }
        builder.ensure();
        KeyAffinityExecutorImpl<K> result = new KeyAffinityExecutorImpl<>(builder::buildInner);
        ALL_EXECUTORS.put(result, wrapStats(result));
        return result;
    }

    /**
     * 是否在关闭时自动关闭执行器，默认为true
     *
     * @param value 是否在关闭时自动关闭执行器
     * @return 当前构造器对象本身
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder shutdownExecutorAfterClose(boolean value) {
        shutdownAfterClose = value;
        return this;
    }

    /**
     * 设置任务按Key分发任务到执行器中，是否为随机挑选执行器执行，一般情况下保持默认值即可，无需手工设置
     *
     * @param value 是否使用随机分发，如果为true使用随机方式分发；为false，则挑选当前执行任务数最少的分发
     * @return 当前构造器对象本身
     * see {@link KeyAffinityBuilder#usingRandom(boolean)}
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder usingRandom(boolean value) {
        builder.usingRandom(value);
        return this;
    }

    /**
     * 设置任务按Key分发任务到执行器中，是否为随机挑选执行器执行，一般情况下使用
     * {@link KeyAffinityExecutor#newSerializingExecutor(int, int, String)}等方法进行构造，无需手工设置
     * {@link IntPredicate}的入参为当前使用的执行器的数量
     *
     * @param value 是否使用随机分发，根据当前执行器的数量判断是否使用随机分发，如果结果为true使用随机方式分发；
     * 为false，则挑选当前执行任务数最少的分发
     * @return 当前构造器对象本身
     * see {@link KeyAffinityBuilder#usingRandom(boolean)}
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder usingRandom(IntPredicate value) {
        builder.usingRandom(value);
        return this;
    }

    /**
     * 执行器提供函数，创建一个指定的执行器对象，一般情况下使用
     * {@link KeyAffinityExecutor#newSerializingExecutor(int, int, String)}等方法进行构造，无需手工设置
     *
     * @param factory 执行器提供函数
     * @return 当前构造器对象本身
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder executor(@Nonnull Supplier<ExecutorService> factory) {
        checkNotNull(factory);
        builder.factory(() -> {
            ExecutorService executor = factory.get();
            if (executor instanceof ListeningExecutorService) {
                return (ListeningExecutorService) executor;
            } else if (executor instanceof ThreadPoolExecutor) {
                return new ThreadListeningExecutorService((ThreadPoolExecutor) executor);
            } else {
                return listeningDecorator(executor);
            }
        });
        return this;
    }

    /**
     * 已废弃：同{@link #parallelism(int)}，语义更清晰
     *
     * @param count 设置并发度，即有多少个执行器来处理不同的Key的任务
     * @return 当前构造器对象本身
     */
    @Deprecated
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder count(int count) {
        return parallelism(count);
    }

    /**
     * 设置并发度，即有多少个执行器来处理不同的Key的任务。由于通常任务执行器的线程数为1，在这种情况下也可以理解成有多少个线程在处理相应的任务
     * 要动态设值请使用{@link #parallelism(IntSupplier)}
     *
     * @param value 设置并发度，即有多少个执行器来处理不同的Key的任务
     * @return 当前构造器对象本身
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder parallelism(int value) {
        builder.count(value);
        return this;
    }

    /**
     * 是否启用计数器检查来保证执行器的个数符合预期，默认开启
     * <p>当使用动态配置并发度的情况下（使用{@link #parallelism(IntSupplier)}动态设置）的情况下，此项务必开启</p>
     *
     * @param value 当前是否开启计数器检查
     * @return 当前构造器对象本身
     */
    @CheckReturnValue
    @Nonnull
    @VisibleForTesting
    KeyAffinityExecutorBuilder counterChecker(BooleanSupplier value) {
        builder.counterChecker(value);
        return this;
    }

    /**
     * 已废弃：同{@link #parallelism(IntSupplier)}，语义更清晰
     *
     * @param count 设置并发度，即有多少个执行器来处理不同的Key的任务
     * @return 当前构造器对象本身
     */
    @Deprecated
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder count(IntSupplier count) {
        return parallelism(count);
    }

    /**
     * 设置并发度，即有多少个执行器来处理不同的Key的任务。由于通常任务执行器的线程数为1，在这种情况下也可以理解成有多少个线程在处理相应的任务
     * {@link #parallelism(int)}的动态设值版本
     *
     * @param count value 设置并发度，即有多少个执行器来处理不同的Key的任务
     * @return 当前构造器对象本身
     */
    @CheckReturnValue
    @Nonnull
    public KeyAffinityExecutorBuilder parallelism(IntSupplier count) {
        builder.count(count);
        usingDynamic = true;
        SimpleRateLimiter rateLimiter = SimpleRateLimiter.create(1);
        builder.counterChecker(rateLimiter::tryAcquire);
        return this;
    }

    /**
     * 获取当前已经创建的所有{@link KeyAffinityExecutor}对象，通常是统计时使用
     *
     * @return 返回所有已经创建的{@link KeyAffinityExecutor}对象集合
     */
    public static Collection<KeyAffinityExecutor<?>> getAllExecutors() {
        return unmodifiableCollection(ALL_EXECUTORS.values());
    }

    @VisibleForTesting
    static void clearAllExecutors() {
        ALL_EXECUTORS.clear();
    }
}
