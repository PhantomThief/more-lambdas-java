/**
 * 
 */
package com.github.phantomthief.util;

import com.google.common.base.Throwables;

/**
 * @author w.vela
 */
public class MoreFunctions {

    public interface FunctionWithThrowable<T, R, X extends Throwable> {

        R apply(T t) throws X;
    }

    public static final <T, R> R catching(FunctionWithThrowable<T, R, Throwable> function, T t) {
        return catching(function, t, (Class<Throwable>) null);
    }

    @SafeVarargs
    public static final <T, R> R catching(FunctionWithThrowable<T, R, Throwable> function, T t,
            Class<? extends Throwable>... catchThrowables) {
        try {
            return function.apply(t);
        } catch (Throwable e) {
            if (catchThrowables != null) {
                for (Class<? extends Throwable> throwable : catchThrowables) {
                    if (throwable != null) {
                        if (throwable.isInstance(e)) {
                            continue;
                        } else {
                            throw Throwables.propagate(e);
                        }
                    }
                }
            }
            return null;
        }
    }
}
