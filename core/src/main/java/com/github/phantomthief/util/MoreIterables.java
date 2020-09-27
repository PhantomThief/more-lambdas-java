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
 * MoreIterables增强工具集合
 * <p>迭代器快捷工具，用于快速获取批次数列</p>
 *
 * @author w.vela
 */
public final class MoreIterables {

    /**
     * 工具类，禁止实例化成对象
     */
    private MoreIterables() {
        throw new UnsupportedOperationException();
    }

    /**
     * 取得一个闭区间的long类型的数列，并按批次的大小进行分组，返回一个{@link Stream}
     *
     * @param from 起始值（含）
     * @param to 终止值（含）
     * @param batch 分组批次大小
     * @return 分组序列Stream
     */
    public static Stream<List<Long>> batchClosedRangeStream(long from, long to, int batch) {
        return toStream(batchClosedRange(from, to, batch));
    }

    /**
     * 取得一个闭区间的long类型的数列，并按批次的大小进行分组，返回一个{@link Iterable}
     *
     * @param from 起始值（含）
     * @param to 终止值（含）
     * @param batch 分组批次大小
     * @return 分组序列Iterable
     */
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

    /**
     * 取得一个闭区间的long类型的数列，并按批次的大小进行分组，返回一个{@link Stream}，使用{@link Range}承载
     *
     * @param from 起始值（含）
     * @param to 终止值（含）
     * @param batch 分组批次大小
     * @return 分组序列Stream
     */
    public static Stream<Range<Long>> batchClosedSimpleRangeStream(long from, long to, int batch) {
        return toStream(batchClosedSimpleRange(from, to, batch));
    }

    /**
     * 取得一个闭区间的long类型的数列，并按批次的大小进行分组，返回一个{@link Iterable}，使用{@link Range}承载
     *
     * @param from 起始值（含）
     * @param to 终止值（含）
     * @param batch 分组批次大小
     * @return 分组序列Iterable
     */
    public static Iterable<Range<Long>> batchClosedSimpleRange(long from, long to, int batch) {
        checkArgument(batch > 0);
        if (from == to) {
            return singleton(Range.closed(from, to));
        }
        boolean reversed = from > to;
        return () -> new Iterator<Range<Long>>() {

            private Range<Long> current = reversed ? Range.closed((max(from - batch, to) + 1), from)
                                                   : Range.closed(from, min(batch + from, to) - 1);

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
