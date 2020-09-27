package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

/**
 * MoreFunctions增强工具集合
 * <p>Java执行函数相关的快捷工具集合，方便运行的异常处理等</p>
 *
 * @author w.vela
 */
public final class MoreFunctions {

    private static final Logger logger = getLogger(MoreFunctions.class);
    private static final String FAIL_SAFE_MARK = "[fail safe]";

    /**
     * 执行一个{@link Callable}，使用fail-safe模式，异常只记录到日志中，返回一个{@link Optional}对象
     *
     * @param callable 要执行的函数
     * @param <R> {@link Callable}返回值类型泛型
     * @return 执行的返回值{@link Optional}对象
     */
    public static <R> Optional<R> catchingOptional(Callable<R> callable) {
        return ofNullable(catching(callable));
    }

    /**
     * 执行一个{@link Callable}，使用fail-safe模式，异常只记录到日志中
     *
     * @param callable 要执行的函数
     * @param <R> {@link Callable}返回值类型泛型
     * @return 执行的返回值
     */
    public static <R> R catching(Callable<R> callable) {
        return catching(callable, e -> logger.error(FAIL_SAFE_MARK, e));
    }

    /**
     * 执行一个{@link ThrowableRunnable}，使用fail-safe模式，异常只记录到日志中
     *
     * @param callable 要执行的函数
     * @param <X> 声明的异常类型
     */
    public static <X extends Exception> void runCatching(ThrowableRunnable<X> callable) {
        catching(() -> {
            callable.run();
            return null;
        }, e -> logger.error(FAIL_SAFE_MARK, e));
    }

    /**
     * 执行一个{@link Callable}，并将检查的异常转换为非检查的异常
     *
     * @param callable 要执行的函数
     * @param <R> {@link Callable}返回值类型泛型
     * @return 执行的返回值
     */
    public static <R> R throwing(Callable<R> callable) {
        return catching(callable, throwable -> {
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        });
    }

    /**
     * 执行一个{@link ThrowableRunnable}，并将检查的异常转换为非检查的异常
     *
     * @param callable 要执行的函数
     * @param <X> 异常类型泛型
     */
    public static <X extends Exception> void runThrowing(ThrowableRunnable<X> callable) {
        catching(() -> {
            callable.run();
            return null;
        }, throwable -> {
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        });
    }

    /**
     * 执行一个{@link Callable}，使用独立的异常处理器处理异常，比直接try-catch更易读
     *
     * @param callable 要执行的函数
     * @param exceptionHandler 异常处理器
     * @param <R> {@link Callable}返回值类型泛型
     * @param <X> 异常类型泛型
     * @return 执行的返回值
     * @throws X 抛出的异常
     */
    @Deprecated
    public static <R, X extends Throwable> R catching(Callable<R> callable,
            ThrowableConsumer<Throwable, X> exceptionHandler) throws X {
        try {
            return callable.call();
        } catch (Throwable e) {
            exceptionHandler.accept(e);
            return null;
        }
    }

    /**
     * 执行一个{@link ThrowableFunction}，使用fail-safe模式，异常只记录到日志中
     *
     * @param function 要执行的函数
     * @param t 函数入参
     * @param <T> 函数入参类型泛型
     * @param <R> {@link ThrowableFunction}返回值类型泛型
     * @return 执行的返回值
     */
    public static <T, R> R catching(ThrowableFunction<T, R, Exception> function, T t) {
        return catching(function, t, e -> logger.error(FAIL_SAFE_MARK, e));
    }

    /**
     * 执行一个{@link ThrowableFunction}，并将检查的异常转换为非检查的异常
     *
     * @param function 要执行的函数
     * @param t 函数入参
     * @param <T> 函数入参类型泛型
     * @param <R> {@link ThrowableFunction}返回值类型泛型
     * @return 执行的返回值
     */
    public static <T, R> R throwing(ThrowableFunction<T, R, Exception> function, T t) {
        return catching(function, t, throwable -> {
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        });
    }

    /**
     * 执行一个{@link ThrowableFunction}，使用独立的异常处理器处理异常，比直接try-catch更易读
     *
     * @param function 要执行的函数
     * @param t 函数入参
     * @param exceptionHandler 异常处理器
     * @param <T> 函数入参类型泛型
     * @param <R> {@link ThrowableFunction}返回值类型泛型
     * @param <X> 异常类型泛型
     * @return 执行的返回值
     * @throws X 抛出的异常
     */
    @Deprecated
    public static <T, R, X extends Throwable> R catching(
            ThrowableFunction<T, R, Exception> function, T t,
            ThrowableConsumer<Throwable, X> exceptionHandler) throws X {
        try {
            return function.apply(t);
        } catch (Throwable e) {
            exceptionHandler.accept(e);
            return null;
        }
    }

    /**
     * 使用指定的{@link ForkJoinPool}运行任务
     *
     * @param pool ForkJoinPool线程池
     * @param func 执行函数
     * @param <X> 执行函数的声明异常类型泛型
     * @throws X 抛出的异常
     * @see #supplyParallel(ForkJoinPool, ThrowableSupplier)
     */
    public static <X extends Throwable> void runParallel(ForkJoinPool pool,
            ThrowableRunnable<X> func) throws X {
        supplyParallel(pool, () -> {
            func.run();
            return null;
        });
    }

    /**
     * 使用指定的{@link ForkJoinPool}运行任务
     * mainly use for {@link Stream#parallel()} with specific thread pool
     * see https://stackoverflow.com/questions/21163108/custom-thread-pool-in-java-8-parallel-stream
     *
     * @param pool ForkJoinPool线程池
     * @param func 执行函数
     * @param <R> 执行函数的返回值类型泛型
     * @param <X> 执行函数的异常类型泛型
     */
    public static <R, X extends Throwable> R supplyParallel(ForkJoinPool pool,
            ThrowableSupplier<R, X> func) throws X {
        checkNotNull(pool);
        Throwable[] throwable = { null };
        ForkJoinTask<R> task = pool.submit(() -> {
            try {
                return func.get();
            } catch (Throwable e) {
                throwable[0] = e;
                return null;
            }
        });
        R r;
        try {
            r = task.get();
        } catch (ExecutionException | InterruptedException impossible) {
            throw new AssertionError(impossible);
        }
        if (throwable[0] != null) {
            //noinspection unchecked
            throw (X) throwable[0];
        }
        return r;
    }

    /**
     * 在当前线程中使用指定的线程名称运行指定的任务，任务运行结束后改回原名称
     * <p>用于显式地标识当前线程正在执行的任务，方便通过jstack等Debug时确定线程的用途。请在确实需要的情况下使用。</p>
     *
     * @param name 线程名称处理函数，入参是当前线程名称，出参是目标线程名称
     * @param func 任务提供器
     * @param <X> 任务运行的异常类型泛型
     * @throws X 任务运行的异常
     */
    public static <X extends Throwable> void runWithThreadName(
            @Nonnull Function<String, String> name, @Nonnull ThrowableRunnable<X> func) throws X {
        supplyWithThreadName(name, () -> {
            func.run();
            return null;
        });
    }

    /**
     * 在当前线程中使用指定的线程名称运行指定的任务，任务运行结束后改回原名称
     * <p>用于显式地标识当前线程正在执行的任务，方便通过jstack等Debug时确定线程的用途。请在确实需要的情况下使用。</p>
     *
     * @param name 线程名称处理函数，入参是当前线程名称，出参是目标线程名称
     * @param func 任务提供器
     * @param <X> 任务运行的异常类型泛型
     * @param <T> 任务运行结果的类型泛型
     * @return 任务运行结果
     * @throws X 任务运行的异常
     */
    public static <X extends Throwable, T> T supplyWithThreadName(
            @Nonnull Function<String, String> name, @Nonnull ThrowableSupplier<T, X> func)
            throws X {
        Thread currentThread = Thread.currentThread();
        String originalThreadName = currentThread.getName();
        String newName = name.apply(originalThreadName);
        if (newName != null) {
            currentThread.setName(newName);
        }
        try {
            return func.get();
        } finally {
            currentThread.setName(originalThreadName);
        }
    }

    /**
     * 返回一个入参为{@link Entry}类型的单参数函数，用于将{@link Entry}映射为以Key-Value为入参的双参数函数
     *
     * @param func 双参数函数
     * @param <K> 键类型泛型
     * @param <V> 值类型泛型
     * @param <T> 返回值类型泛型
     * @return 入参为{@link Entry}类型的单参数函数
     */
    public static <K, V, T> Function<Entry<K, V>, T> mapKv(BiFunction<K, V, T> func) {
        return entry -> func.apply(entry.getKey(), entry.getValue());
    }

    /**
     * 返回一个入参为{@link Entry}类型的单参数{@link Predicate}，用于将{@link Entry}映射为以Key-Value为入参的双参数{@link BiPredicate}
     *
     * @param func 双参数{@link BiPredicate}
     * @param <K> 键类型泛型
     * @param <V> 值类型泛型
     * @return 入参为{@link Entry}类型的单参数{@link Predicate}
     */
    public static <K, V> Predicate<Entry<K, V>> filterKv(BiPredicate<K, V> func) {
        return entry -> func.test(entry.getKey(), entry.getValue());
    }

    /**
     * 返回一个入参为{@link Entry}类型的单参数{@link Consumer}，用于将{@link Entry}映射为以Key-Value为入参的双参数{@link BiConsumer}
     *
     * @param func 双参数{@link BiConsumer}
     * @param <K> 键类型泛型
     * @param <V> 值类型泛型
     * @return 入参为{@link Entry}类型的单参数{@link Consumer}
     */
    public static <K, V> Consumer<Entry<K, V>> consumerKv(BiConsumer<K, V> func) {
        return entry -> func.accept(entry.getKey(), entry.getValue());
    }
}
