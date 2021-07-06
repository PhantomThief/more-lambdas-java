package com.github.phantomthief.concurrent;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phantomthief.util.ThrowableConsumer;
import com.github.phantomthief.util.ThrowableFunction;
import com.github.phantomthief.util.ThrowableRunnable;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;

/**
 * MoreFutures增强工具集合
 * <p>用于简化{@link Future}相关的操作</p>
 *
 * @author w.vela
 * Created on 2018-06-25.
 */
public class MoreFutures {

    private static final Logger logger = LoggerFactory.getLogger(MoreFutures.class);

    /**
     * 获取并返回一个{@link Future}的操作结果值，并将可能出现的异常以非检查异常的方式抛出
     *
     * @param future 要获取值的{@link Future}
     * @param duration 获取值的超时时间
     * @throws UncheckedTimeoutException 操作超时异常
     * @throws java.util.concurrent.CancellationException 任务取消异常
     * @throws ExecutionError 执行时发生的{@link Error}异常
     * @throws UncheckedExecutionException 其他执行异常
     */
    public static <T> T getUnchecked(@Nonnull Future<? extends T> future,
            @Nonnull Duration duration) {
        checkNotNull(duration);
        return getUnchecked(future, duration.toNanos(), NANOSECONDS);
    }

    /**
     * 获取并返回一个{@link Future}的操作结果值，并将可能出现的异常以非检查异常的方式抛出
     *
     * @param future 要获取值的{@link Future}
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @throws UncheckedTimeoutException 操作超时异常
     * @throws java.util.concurrent.CancellationException 任务取消异常
     * @throws ExecutionError 执行时发生的{@link Error}异常
     * @throws UncheckedExecutionException 其他执行异常
     */
    public static <T> T getUnchecked(@Nonnull Future<? extends T> future, @Nonnegative long timeout,
            @Nonnull TimeUnit unit) {
        return getUnchecked(future, timeout, unit, false);
    }

    /**
     * 获取并返回一个{@link Future}的操作结果值，并将可能出现的异常以非检查异常的方式抛出
     *
     * @param future 要获取值的{@link Future}
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @param cancelOnTimeout 执行超时后是否取消{@link Future}的执行
     * @throws UncheckedTimeoutException 操作超时异常
     * @throws java.util.concurrent.CancellationException 任务取消异常
     * @throws ExecutionError 执行时发生的{@link Error}异常
     * @throws UncheckedExecutionException 其他执行异常
     */
    public static <T> T getUnchecked(@Nonnull Future<? extends T> future, @Nonnegative long timeout,
            @Nonnull TimeUnit unit, boolean cancelOnTimeout) {
        checkArgument(timeout > 0);
        checkNotNull(future);
        try {
            return getUninterruptibly(future, timeout, unit);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw new ExecutionError((Error) cause);
            } else {
                throw new UncheckedExecutionException(cause);
            }
        } catch (TimeoutException e) {
            if (cancelOnTimeout) {
                future.cancel(false);
            }
            throw new UncheckedTimeoutException(e);
        }
    }

    /**
     * 同时获取并返回一批{@link Future}的操作结果值
     *
     * @param futures 要获取值的多个{@link Future}
     * @param duration 获取值的超时时间
     * @throws TryWaitFutureUncheckedException 当并非所有的{@link Future}都成功返回时，将会抛出此异常，
     * 可以通过此异常获取成功、异常、超时、取消的各个{@link Future}
     * @see #tryWait(Iterable, long, TimeUnit)
     */
    @Nonnull
    public static <F extends Future<V>, V> Map<F, V> tryWait(@Nonnull Iterable<F> futures,
            @Nonnull Duration duration) throws TryWaitFutureUncheckedException {
        checkNotNull(futures);
        checkNotNull(duration);
        return tryWait(futures, duration.toNanos(), NANOSECONDS);
    }

    /**
     * 同时获取并返回一批{@link Future}的操作结果值
     * <p>典型的使用场景：</p>
     * <pre>{@code
     *  // a fail-safe example
     *  List<Future<User>> list = doSomeAsyncTasks();
     *  Map<Future<User>, User> success;
     *  try {
     *    success = tryWait(list, 1, SECONDS);
     *  } catch (TryWaitFutureUncheckedException e) {
     *    success = e.getSuccess(); // there are still some success
     *  }
     *
     *  // a fail-fast example
     *  List<Future<User>> list = doSomeAsyncTasks();
     *  // don't try/catch the exception it throws.
     *  Map<Future<User>, User> success = tryWait(list, 1, SECONDS);
     * }</pre>
     *
     * @param futures 要获取值的多个{@link Future}
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @throws TryWaitFutureUncheckedException 当并非所有的{@link Future}都成功返回时，将会抛出此异常，
     * 可以通过此异常获取成功、异常、超时、取消的各个{@link Future}
     */
    @Nonnull
    public static <F extends Future<V>, V> Map<F, V> tryWait(@Nonnull Iterable<F> futures, @Nonnegative long timeout,
            @Nonnull TimeUnit unit) throws TryWaitFutureUncheckedException {
        checkNotNull(futures);
        checkArgument(timeout > 0);
        checkNotNull(unit);
        return tryWait(futures, timeout, unit, it -> it, TryWaitFutureUncheckedException::new);
    }

    /**
     * 同时获取并返回一批{@link Future}的操作结果值
     *
     * @param keys 要获取值的Key，作为输入值，通过asyncFunc参数传入的函数转换为Future对象
     * @param duration 获取值的超时时间
     * @param asyncFunc 异步转换函数，将输入的每一个Key值转换为{@link Future}
     * @throws TryWaitUncheckedException if not all calls are successful.
     * @see #tryWait(Iterable, long, TimeUnit, ThrowableFunction)
     */
    @Nonnull
    public static <K, V, X extends Throwable> Map<K, V> tryWait(@Nonnull Iterable<K> keys,
            @Nonnull Duration duration, @Nonnull ThrowableFunction<K, Future<V>, X> asyncFunc)
            throws X, TryWaitUncheckedException {
        checkNotNull(keys);
        checkNotNull(duration);
        checkNotNull(asyncFunc);
        return tryWait(keys, duration.toNanos(), NANOSECONDS, asyncFunc);
    }

    /**
     * 同时获取并返回一批{@link Future}的操作结果值
     * <p>典型的使用场景：</p>
     * <pre>{@code
     *  // a fail-safe example
     *  List<Integer> list = getSomeIds();
     *  Map<Integer, User> success;
     *  try {
     *    // 注意这里的线程池executor.submit操作可能失败和阻塞，需要妥善处理
     *    success = tryWait(list, 1, SECONDS, id -> executor.submit(() -> retrieve(id)));
     *  } catch (TryWaitUncheckedException e) {
     *    success = e.getSuccess(); // there are still some success
     *  }
     *
     *  // a fail-fast example
     *  List<Integer> list = getSomeIds();
     *  // don't try/catch the exception it throws.
     *  // 注意这里的线程池executor.submit操作可能失败和阻塞，需要妥善处理
     *  Map<Integer, User> success = tryWait(list, 1, SECONDS, id -> executor.submit(() -> retrieve(id)));
     * }</pre>
     *
     * @param keys 要获取值的Key，作为输入值，通过asyncFunc参数传入的函数转换为Future对象
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @param asyncFunc 异步转换函数，将输入的每一个Key值转换为{@link Future}
     * @throws TryWaitUncheckedException if not all calls are successful.
     */
    @Nonnull
    public static <K, V, X extends Throwable> Map<K, V> tryWait(@Nonnull Iterable<K> keys,
            @Nonnegative long timeout, @Nonnull TimeUnit unit,
            @Nonnull ThrowableFunction<K, Future<V>, X> asyncFunc)
            throws X, TryWaitUncheckedException {
        return tryWait(keys, timeout, unit, asyncFunc, TryWaitUncheckedException::new);
    }

    @Nonnull
    private static <K, V, X extends Throwable> Map<K, V> tryWait(@Nonnull Iterable<K> keys,
            @Nonnegative long timeout, @Nonnull TimeUnit unit,
            @Nonnull ThrowableFunction<K, Future<V>, X> asyncFunc,
            @Nonnull Function<TryWaitResult, RuntimeException> throwing) throws X {
        checkNotNull(keys);
        checkArgument(timeout > 0);
        checkNotNull(unit);
        checkNotNull(asyncFunc);

        Map<Future<? extends V>, V> successMap = new LinkedHashMap<>();
        Map<Future<? extends V>, Throwable> failMap = new LinkedHashMap<>();
        Map<Future<? extends V>, TimeoutException> timeoutMap = new LinkedHashMap<>();
        Map<Future<? extends V>, CancellationException> cancelMap = new LinkedHashMap<>();

        long remainingNanos = unit.toNanos(timeout);
        long end = nanoTime() + remainingNanos;

        Map<Future<? extends V>, K> futureKeyMap = new IdentityHashMap<>();
        for (K key : keys) {
            checkNotNull(key);
            Future<V> future = asyncFunc.apply(key);
            checkNotNull(future);
            futureKeyMap.put(future, key);
        }
        for (Future<? extends V> future : futureKeyMap.keySet()) {
            if (remainingNanos <= 0) {
                waitAndCollect(successMap, failMap, timeoutMap, cancelMap, future, 1L);
                continue;
            }
            waitAndCollect(successMap, failMap, timeoutMap, cancelMap, future, remainingNanos);
            remainingNanos = end - nanoTime();
        }

        TryWaitResult<K, V> result = new TryWaitResult<>(successMap, failMap, timeoutMap, cancelMap,
                futureKeyMap);

        if (failMap.isEmpty() && timeoutMap.isEmpty() && cancelMap.isEmpty()) {
            return result.getSuccess();
        } else {
            throw throwing.apply(result);
        }
    }

    private static <T> void waitAndCollect(Map<Future<? extends T>, T> successMap,
            Map<Future<? extends T>, Throwable> failMap,
            Map<Future<? extends T>, TimeoutException> timeoutMap,
            Map<Future<? extends T>, CancellationException> cancelMap, Future<? extends T> future,
            long thisWait) {
        try {
            T t = getUninterruptibly(future, thisWait, NANOSECONDS);
            successMap.put(future, t);
        } catch (CancellationException e) {
            cancelMap.put(future, e);
        } catch (TimeoutException e) {
            timeoutMap.put(future, e);
        } catch (ExecutionException e) {
            failMap.put(future, e.getCause());
        } catch (Throwable e) {
            failMap.put(future, e);
        }
    }

    /**
     * 执行一个计划任务，按照上一次计划任务返回的等待时间，来运行下一次任务
     *
     * @param executor 任务执行器，当任务执行器停止时，所有任务将停止运行
     * @param initDelay 首次执行的延迟时间
     * @param task 执行的任务，在任务中抛出的异常会终止任务的运行，需谨慎处理
     * @return 执行任务的Future，可以用来取消任务
     */
    public static Future<?> scheduleWithDynamicDelay(@Nonnull ScheduledExecutorService executor,
            @Nullable Duration initDelay, @Nonnull Scheduled task) {
        checkNotNull(executor);
        checkNotNull(task);
        AtomicBoolean canceled = new AtomicBoolean(false);
        AbstractFuture<?> future = new AbstractFuture<Object>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                canceled.set(true);
                return super.cancel(mayInterruptIfRunning);
            }
        };
        executor.schedule(new ScheduledTaskImpl(executor, task, canceled),
                initDelay == null ? 0 : initDelay.toMillis(), MILLISECONDS);
        return future;
    }

    /**
     * 执行一个计划任务，按照上一次计划任务返回的等待时间，来运行下一次任务
     *
     * @param executor 任务执行器，当任务执行器停止时，所有任务将停止运行
     * @param initialDelay 首次执行的延迟时间
     * @param delay 任务延迟提供器，用来设置任务执行之后下一次的执行间隔时间
     * @param task 执行的任务，在任务中抛出的异常会终止任务的运行，需谨慎处理
     * @return 执行任务的Future，可以用来取消任务
     */
    public static Future<?> scheduleWithDynamicDelay(@Nonnull ScheduledExecutorService executor,
            @Nonnull Duration initialDelay, @Nonnull Supplier<Duration> delay,
            @Nonnull ThrowableRunnable<Throwable> task) {
        checkNotNull(initialDelay);
        checkNotNull(delay);
        checkNotNull(task);
        return scheduleWithDynamicDelay(executor, initialDelay, () -> {
            try {
                task.run();
            } catch (Throwable e) {
                logger.error("", e);
            }
            return delay.get();
        });
    }

    /**
     * 执行一个计划任务，按照上一次计划任务返回的等待时间，来运行下一次任务
     *
     * @param executor 任务执行器，当任务执行器停止时，所有任务将停止运行
     * @param delay 任务延迟提供器，用来设置任务执行之后下一次的执行间隔时间
     * @param task 执行的任务，在任务中抛出的异常会终止任务的运行，需谨慎处理
     * @return 执行任务的Future，可以用来取消任务
     */
    public static Future<?> scheduleWithDynamicDelay(@Nonnull ScheduledExecutorService executor,
            @Nonnull Supplier<Duration> delay, @Nonnull ThrowableRunnable<Throwable> task) {
        checkNotNull(delay);
        return scheduleWithDynamicDelay(executor, delay.get(), () -> {
            try {
                task.run();
            } catch (Throwable e) {
                logger.error("", e);
            }
            return delay.get();
        });
    }

    /**
     * 用于替换 {@link Futures#transform(ListenableFuture, com.google.common.base.Function, Executor)}
     * <p>
     * 主要提供两个额外的功能:
     * 1. API使用jdk8
     * 2. 提供了 {@link TimeoutListenableFuture} 的支持（保持Listener不会丢）
     *
     * @param input 输入的Future
     * @param function 用于从输入的Future转换为输出Future结果类型的转换函数
     * @param executor 转换函数执行的Executor
     * @return 返回转换后的Future对象
     */
    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input,
            Function<? super I, ? extends O> function, Executor executor) {
        @SuppressWarnings("Guava")
        com.google.common.base.Function<? super I, ? extends O> realFunc;
        if (function instanceof com.google.common.base.Function) {
            //noinspection unchecked
            realFunc = (com.google.common.base.Function) function;
        } else {
            realFunc = function::apply;
        }
        ListenableFuture<O> result = Futures.transform(input, realFunc, executor);
        if (input instanceof TimeoutListenableFuture) {
            TimeoutListenableFuture<O> newResult = new TimeoutListenableFuture<>(result);
            for (ThrowableConsumer<TimeoutException, Exception> timeoutListener : ((TimeoutListenableFuture<I>) input)
                    .getTimeoutListeners()) {
                newResult.addTimeoutListener(timeoutListener);
            }
            return newResult;
        } else {
            return result;
        }
    }

    /**
     * 用于替换
     * {@link Futures#transformAsync(ListenableFuture, com.google.common.util.concurrent.AsyncFunction, Executor)}
     * <p>
     * 主要提供两个额外的功能:
     * 1. API使用jdk8
     * 2. 提供了 {@link TimeoutListenableFuture} 的支持（保持Listener不会丢）
     *
     * @param input 输入的Future
     * @param function 用于从输入的Future转换为输出Future结果类型的转换函数
     * @param executor 转换函数执行的Executor
     * @return 返回转换后的Future对象
     */
    public static <I, O> ListenableFuture<O> transformAsync(ListenableFuture<I> input,
            AsyncFunction<? super I, ? extends O> function,
            Executor executor) {
        ListenableFuture<O> result = Futures.transformAsync(input, function, executor);
        if (input instanceof TimeoutListenableFuture) {
            TimeoutListenableFuture<O> newResult = new TimeoutListenableFuture<>(result);
            for (ThrowableConsumer<TimeoutException, Exception> timeoutListener : ((TimeoutListenableFuture<I>) input)
                    .getTimeoutListeners()) {
                newResult.addTimeoutListener(timeoutListener);
            }
            return newResult;
        } else {
            return result;
        }
    }

    public interface Scheduled {

        /**
         * @return a delay for next run. {@code null} means stop.
         */
        @Nullable
        Duration run();
    }

    private static class ScheduledTaskImpl implements Runnable {

        private final ScheduledExecutorService executorService;
        private final Scheduled scheduled;
        private final AtomicBoolean canceled;

        private ScheduledTaskImpl(ScheduledExecutorService executorService, Scheduled scheduled,
                AtomicBoolean canceled) {
            this.executorService = executorService;
            this.scheduled = scheduled;
            this.canceled = canceled;
        }

        @Override
        public void run() {
            if (canceled.get()) {
                return;
            }
            try {
                Duration delay = scheduled.run();
                if (!canceled.get() && delay != null) {
                    executorService.schedule(this, delay.toMillis(), MILLISECONDS);
                }
            } catch (Throwable e) {
                logger.error("", e);
            }
        }
    }
}
