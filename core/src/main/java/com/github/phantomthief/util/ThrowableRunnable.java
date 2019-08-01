package com.github.phantomthief.util;

/**
 * 
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableRunnable<X extends Throwable> {

    void run() throws X;
}
