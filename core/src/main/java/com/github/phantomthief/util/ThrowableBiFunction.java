package com.github.phantomthief.util;

/**
 * 
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableBiFunction<T, U, R, X extends Throwable> {

    R apply(T t, U u) throws X;
}
