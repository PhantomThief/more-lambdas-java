/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author w.vela
 */
public class MoreStreams {

    private MoreStreams() {
        throw new UnsupportedOperationException();
    }

    public static LongStream longRangeClosed(long from, long to) {
        if (from <= to) {
            return LongStream.rangeClosed(from, to);
        } else {
            return LongStream.rangeClosed(to, from).map(i -> to - i + from);
        }
    }

    public static IntStream intRangeClosed(int from, int to) {
        if (from <= to) {
            return IntStream.rangeClosed(from, to);
        } else {
            return IntStream.rangeClosed(to, from).map(i -> to - i + from);
        }
    }

    public static <T> Stream<T> toStream(Iterator<T> iterator) {
        checkNotNull(iterator);
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator,
                        (Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.ORDERED)),
                false);
    }

    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        checkNotNull(iterable);
        if (iterable instanceof Collection) {
            // failfast
            try {
                Collection<T> collection = (Collection<T>) iterable;
                return StreamSupport.stream(Spliterators.spliterator(collection, 0), false);
            } catch (Throwable e) {
                // do nothing
            }
        }
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterable.iterator(),
                        (Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.ORDERED)),
                false);
    }
}
