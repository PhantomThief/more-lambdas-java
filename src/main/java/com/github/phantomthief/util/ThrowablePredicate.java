package com.github.phantomthief.util;

/**
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowablePredicate<T, X extends Throwable> {

    boolean test(T t) throws X;
}
