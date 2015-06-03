/**
 * 
 */
package com.github.phantomthief.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author w.vela
 */
public final class MorePredicates {

    private MorePredicates() {
        throw new UnsupportedOperationException();
    }

    public static final <T> Predicate<T> distinctUsing(Function<T, Object> mapper) {
        return new Predicate<T>() {

            private final Set<Object> set = new HashSet<>();

            @Override
            public boolean test(T t) {
                return set.add(mapper.apply(t));
            }
        };
    }

    public static <T> Predicate<T> after(T element) {
        return after(element, true);
    }

    public static <T> Predicate<T> after(T element, boolean inclusive) {
        return new Predicate<T>() {

            private boolean started = element == null;

            @Override
            public boolean test(T t) {
                if (started) {
                    return true;
                } else {
                    if (Objects.equals(t, element)) {
                        started = true;
                        if (inclusive) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        };
    }

}
