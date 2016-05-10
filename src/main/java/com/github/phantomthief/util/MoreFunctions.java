/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Throwables.propagate;
import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author w.vela
 */
public class MoreFunctions {

    public static <R> Optional<R> safe(Callable<R> callable) {
        return safe(callable, (Class<Throwable>) null);
    }

    public static <R> R catching(Callable<R> callable) {
        return catching(callable, (Class<Throwable>) null);
    }

    @SafeVarargs
    public static <R> Optional<R> safe(Callable<R> callable,
            Class<? extends Throwable>... catchThrowables) {
        return ofNullable(catching(callable, catchThrowables));
    }

    @SafeVarargs
    public static <R> R catching(Callable<R> callable,
            Class<? extends Throwable>... catchThrowables) {
        try {
            return callable.call();
        } catch (Throwable e) {
            if (catchThrowables != null) {
                for (Class<? extends Throwable> throwable : catchThrowables) {
                    if (throwable != null) {
                        if (!throwable.isInstance(e)) {
                            throw propagate(e);
                        }
                    }
                }
            }
            return null;
        }
    }

    public static <T, R> Optional<R> safe(FunctionWithThrowable<T, R, Throwable> function, T t) {
        return safe(function, t, (Class<Throwable>) null);
    }

    public static <T, R> R catching(FunctionWithThrowable<T, R, Throwable> function, T t) {
        return catching(function, t, (Class<Throwable>) null);
    }

    @SafeVarargs
    public static <T, R> Optional<R> safe(FunctionWithThrowable<T, R, Throwable> function, T t,
            Class<? extends Throwable>... catchThrowables) {
        return ofNullable(catching(function, t, catchThrowables));
    }

    @SafeVarargs
    public static <T, R> R catching(FunctionWithThrowable<T, R, Throwable> function, T t,
            Class<? extends Throwable>... catchThrowables) {
        try {
            return function.apply(t);
        } catch (Throwable e) {
            if (catchThrowables != null) {
                for (Class<? extends Throwable> throwable : catchThrowables) {
                    if (throwable != null) {
                        if (!throwable.isInstance(e)) {
                            throw propagate(e);
                        }
                    }
                }
            }
            return null;
        }
    }

    public interface FunctionWithThrowable<T, R, X extends Throwable> {

        R apply(T t) throws X;
    }
}
