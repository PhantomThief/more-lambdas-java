package com.github.phantomthief.util;

import static java.util.Objects.requireNonNull;

/**
 * 
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableFunction<T, R, X extends Throwable> {

    R apply(T t) throws X;

    default <V> ThrowableFunction<V, R, X>
            compose(ThrowableFunction<? super V, ? extends T, X> before) {
        requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    default <V> ThrowableFunction<T, V, X> andThen(ThrowableFunction<? super R, ? extends V, X> after) {
        requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    static <T, X extends Throwable> ThrowableFunction<T, T, X> identity() {
        return t -> t;
    }
}
