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
 * @author w.vela
 */
public final class MoreFunctions {

    private static final Logger logger = getLogger(MoreFunctions.class);

    public static <R> Optional<R> catchingOptional(Callable<R> callable) {
        return ofNullable(catching(callable));
    }

    public static <R> R catching(Callable<R> callable) {
        return catching(callable, e -> logger.error("", e));
    }

    public static <X extends Exception> void runCatching(ThrowableRunnable<X> callable) {
        catching(() -> {
            callable.run();
            return null;
        }, e -> logger.error("", e));
    }

    public static <R> R throwing(Callable<R> callable) {
        return catching(callable, throwable -> {
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        });
    }

    public static <X extends Exception> void runThrowing(ThrowableRunnable<X> callable) {
        catching(() -> {
            callable.run();
            return null;
        }, throwable -> {
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        });
    }

    public static <R, X extends Throwable> R catching(Callable<R> callable,
            ThrowableConsumer<Throwable, X> exceptionHandler) throws X {
        try {
            return callable.call();
        } catch (Throwable e) {
            exceptionHandler.accept(e);
            return null;
        }
    }

    public static <T, R> R catching(ThrowableFunction<T, R, Throwable> function, T t) {
        return catching(function, t, e -> logger.error("", e));
    }

    public static <T, R> R throwing(ThrowableFunction<T, R, Throwable> function, T t) {
        return catching(function, t, throwable -> {
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        });
    }

    public static <T, R, X extends Throwable> R catching(
            ThrowableFunction<T, R, Throwable> function, T t,
            ThrowableConsumer<Throwable, X> exceptionHandler) throws X {
        try {
            return function.apply(t);
        } catch (Throwable e) {
            exceptionHandler.accept(e);
            return null;
        }
    }

    /**
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
     * mainly use for {@link Stream#parallel()} with specific thread pool
     * see https://stackoverflow.com/questions/21163108/custom-thread-pool-in-java-8-parallel-stream
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

    public static <X extends Throwable> void runWithThreadName(
            @Nonnull Function<String, String> name, @Nonnull ThrowableRunnable<X> func) throws X {
        supplyWithThreadName(name, () -> {
            func.run();
            return null;
        });
    }

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

    public static <K, V, T> Function<Entry<K, V>, T> mapKv(BiFunction<K, V, T> func) {
        return entry -> func.apply(entry.getKey(), entry.getValue());
    }

    public static <K, V> Predicate<Entry<K, V>> filterKv(BiPredicate<K, V> func) {
        return entry -> func.test(entry.getKey(), entry.getValue());
    }

    public static <K, V> Consumer<Entry<K, V>> consumerKv(BiConsumer<K, V> func) {
        return entry -> func.accept(entry.getKey(), entry.getValue());
    }
}
