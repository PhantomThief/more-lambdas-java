/**
 * 
 */
package com.github.phantomthief.util;

import java.util.HashSet;
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

}
