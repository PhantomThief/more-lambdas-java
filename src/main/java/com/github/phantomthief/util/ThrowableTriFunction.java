/**
 * 
 */
package com.github.phantomthief.util;

/**
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableTriFunction<T1, T2, T3, R, X extends Throwable> {

    R apply(T1 t1, T2 t2, T3 t3) throws X;
}
