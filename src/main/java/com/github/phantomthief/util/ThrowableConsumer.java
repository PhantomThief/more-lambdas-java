/**
 * 
 */
package com.github.phantomthief.util;

import static java.util.Objects.requireNonNull;

/**
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableConsumer<T, X extends Throwable> {

    void accept(T t) throws X;

    default ThrowableConsumer<T, X> andThen(ThrowableConsumer<? super T, X> after) {
        requireNonNull(after);
        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}
