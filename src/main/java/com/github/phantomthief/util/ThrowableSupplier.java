/**
 * 
 */
package com.github.phantomthief.util;

/**
 * @author w.vela
 */
public interface ThrowableSupplier<T, X extends Throwable> {

    T get() throws X;
}
