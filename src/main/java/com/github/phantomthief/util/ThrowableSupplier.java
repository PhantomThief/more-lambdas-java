package com.github.phantomthief.util;

/**
 * @author w.vela
 */
public interface ThrowableSupplier<T, X extends Throwable> {

    T get() throws X;

    static <T, X extends Throwable> ThrowableSupplier<T, X> cast(ThrowableSupplier<T, X> func) {
        return func;
    }
}
