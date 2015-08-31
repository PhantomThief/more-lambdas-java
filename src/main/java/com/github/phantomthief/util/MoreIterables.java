/**
 * 
 */
package com.github.phantomthief.util;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.LongStream;

import com.google.common.base.Preconditions;

/**
 * @author w.vela
 */
public class MoreIterables {

    private MoreIterables() {
        throw new UnsupportedOperationException();
    }

    public static Iterable<List<Long>> batchClosedRange(long from, long to, int batch) {
        Preconditions.checkArgument(batch > 0);
        if (from == to) {
            return Collections
                    .singleton(LongStream.rangeClosed(from, to).boxed().collect(toList()));
        }
        boolean reversed = from > to;
        return new Iterable<List<Long>>() {

            @Override
            public Iterator<List<Long>> iterator() {
                return new Iterator<List<Long>>() {

                    private List<Long> current = MoreStreams
                            .longRangeClosed(from, reversed ? (Math.max(from - batch, to)
                                    + 1) : Math.min(batch + from, to) - 1)
                            .boxed().collect(toList());

                    @Override
                    public boolean hasNext() {
                        return current != null && !current.isEmpty();
                    }

                    @Override
                    public List<Long> next() {
                        if (current == null) {
                            throw new NoSuchElementException();
                        }
                        List<Long> result = current;
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
                            newStart = current.get(current.size() - 1) - 1;
                        } else {
                            newStart = current.get(current.size() - 1) + 1;
                        }
                        if ((!reversed && newStart > to) || (reversed && to > newStart)) {
                            current = null;
                            return;
                        }
                        long newEnd;
                        if (reversed) {
                            newEnd = Math.max(to, newStart - batch + 1);
                        } else {
                            newEnd = Math.min(to, newStart + batch - 1);
                        }
                        current = MoreStreams.longRangeClosed(newStart, newEnd).boxed()
                                .collect(toList());
                    }
                };
            }
        };
    }
}
