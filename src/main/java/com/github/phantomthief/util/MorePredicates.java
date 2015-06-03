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
        return after(e -> Objects.equals(element, e), inclusive);
    }

    public static <T> Predicate<T> after(Predicate<T> predicate) {
        return after(predicate, true);
    }

    public static <T> Predicate<T> after(Predicate<T> predicate, boolean inclusive) {
        return new Predicate<T>() {

            private boolean started;

            @Override
            public boolean test(T t) {
                if (started) {
                    return true;
                } else {
                    if (predicate.test(t)) {
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
