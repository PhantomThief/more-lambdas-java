package com.github.phantomthief.util;

/**
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableBiPredicate<T, U, X extends Throwable> {

    boolean test(T t, U u) throws X;
}
