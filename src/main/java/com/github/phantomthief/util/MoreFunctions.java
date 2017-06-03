/**
 * 
 */
package com.github.phantomthief.util;

import static java.util.Optional.ofNullable;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;

import com.google.common.base.Throwables;

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

    public static <X extends Exception> void catching(ThrowableRunnable<X> callable) {
        catching(() -> {
            callable.run();
            return null;
        }, e -> logger.error("", e));
    }

    public static <R> R throwing(Callable<R> callable) {
        return catching(callable, Throwables::propagate);
    }

    public static <X extends Exception> void throwing(ThrowableRunnable<X> callable) {
        catching(() -> {
            callable.run();
            return null;
        }, Throwables::propagate);
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

    public static <T, R> R catching(ThrowableFunction<T, R, Exception> function, T t) {
        return catching(function, t, e -> logger.error("", e));
    }

    public static <T, R> R throwing(ThrowableFunction<T, R, Exception> function, T t) {
        return catching(function, t, Throwables::propagate);
    }

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
}
