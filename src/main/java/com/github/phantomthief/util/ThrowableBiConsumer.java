/**
 * 
 */
package com.github.phantomthief.util;

import static java.util.Objects.requireNonNull;

/**
 * @author w.vela
 */
@FunctionalInterface
public interface ThrowableBiConsumer<T, U, X extends Throwable> {

    void accept(T t, U u) throws X;

    default ThrowableBiConsumer<T, U, X>
            andThen(ThrowableBiConsumer<? super T, ? super U, X> after) {
        requireNonNull(after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
