/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Throwables.propagate;

import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author w.vela
 */
public class MoreFunctions {

    public static <T, R> Function<T, R> naming(Function<T, R> func,
            Supplier<String> toStringSupplier) {
        return new Function<T, R>() {

            @Override
            public R apply(T o) {
                return func.apply(o);
            }

            @Override
            public String toString() {
                return toStringSupplier.get();
            }
        };
    }

    public static <R> R catching(Callable<R> callable) {
        return catching(callable, (Class<Throwable>) null);
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

    public static <T, R> R catching(FunctionWithThrowable<T, R, Throwable> function, T t) {
        return catching(function, t, (Class<Throwable>) null);
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
