package com.github.phantomthief.util;

/**
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableTriConsumer<T1, T2, T3, X extends Throwable> {

    void accept(T1 t1, T2 t2, T3 t3) throws X;
}
