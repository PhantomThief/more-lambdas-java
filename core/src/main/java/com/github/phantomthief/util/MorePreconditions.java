package com.github.phantomthief.util;

import static java.lang.String.format;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author w.vela
 * Created on 2020-02-26.
 */
public final class MorePreconditions {

    /**
     * similar to {@link com.google.common.base.Preconditions#checkArgument(boolean)}
     * with customize exception.
     */
    public static <X extends Throwable> void checkOrThrow(boolean expression, Supplier<X> exception) throws X {
        if (!expression) {
            throw exception.get();
        }
    }

    /**
     * similar to {@link com.google.common.base.Preconditions#checkArgument(boolean, String, Object...)}
     * with customize exception.
     */
    public static <X extends Throwable> void checkOrThrow(boolean expression, Function<String, X> exception,
            String errorMessageTemplate, Object... errorMessageArgs) throws X {
        if (!expression) {
            throw exception.apply(format(errorMessageTemplate, errorMessageArgs));
        }
    }
}
