/**
 * 
 */
package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreStreams.toStream;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.partition;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.collect.Range;

/**
 * @author w.vela
 */
public final class MoreIterables {

    private MoreIterables() {
        throw new UnsupportedOperationException();
    }

    public static Stream<List<Long>> batchClosedRangeStream(long from, long to, int batch) {
        return toStream(batchClosedRange(from, to, batch));
    }

    public static Iterable<List<Long>> batchClosedRange(long from, long to, int batch) {
        checkArgument(batch > 0);
        if (from == to) {
            return singleton(singletonList(from));
        }
        LongStream longStream;
        if (from > to) {
            longStream = LongStream.rangeClosed(to, from).map(i -> from + to - i);
        } else {
            longStream = LongStream.rangeClosed(from, to);
        }
        return partition(longStream.boxed()::iterator, batch);
    }

    public static Stream<Range<Long>> batchClosedSimpleRangeStream(long from, long to, int batch) {
        return toStream(batchClosedSimpleRange(from, to, batch));
    }

    public static Iterable<Range<Long>> batchClosedSimpleRange(long from, long to, int batch) {
        checkArgument(batch > 0);
        if (from == to) {
            return singleton(Range.closed(from, to));
        }
        boolean reversed = from > to;
        return () -> new Iterator<Range<Long>>() {

            private Range<Long> current = reversed ? // 
            Range.closed((max(from - batch, to) + 1), from) : // 
            Range.closed(from, min(batch + from, to) - 1);

            @Override
            public boolean hasNext() {
                return current != null && !current.isEmpty();
            }

            @Override
            public Range<Long> next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                Range<Long> result = current;
                calcNext();
                return result;
            }

            private void calcNext() {
                if (current.isEmpty()) {
                    current = null;
                    return;
                }
                long newStart;
                if (reversed) {
                    newStart = current.lowerEndpoint() - 1;
                } else {
                    newStart = current.upperEndpoint() + 1;
                }
                if ((!reversed && newStart > to) || (reversed && to > newStart)) {
                    current = null;
                    return;
                }
                long newEnd;
                if (reversed) {
                    newEnd = max(to, newStart - batch + 1);
                } else {
                    newEnd = min(to, newStart + batch - 1);
                }
                current = reversed ? Range.closed(newEnd, newStart) : Range.closed(newStart,
                        newEnd);
            }
        };
    }
}
