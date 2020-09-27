package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliterator;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.StreamSupport.stream;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

/**
 * MoreStreams增强工具集合
 * <p>帮助使用JavaStream，提供一些便利的工具方法</p>
 *
 * @author w.vela
 */
public class MoreStreams {

    /**
     * 工具类，禁止实例化成对象
     */
    private MoreStreams() {
        throw new UnsupportedOperationException();
    }

    /**
     * 创建一个类型为long的Stream，返回从开始到结束一个闭区间内的整数
     *
     * @param from 起始值（含）
     * @param to 终止值（含）
     * @return 生成的{@link LongStream}
     */
    public static LongStream longRangeClosed(long from, long to) {
        if (from <= to) {
            return LongStream.rangeClosed(from, to);
        } else {
            return LongStream.rangeClosed(to, from).map(i -> to - i + from);
        }
    }

    /**
     * 创建一个类型为int的Stream，返回从开始到结束一个闭区间内的整数
     *
     * @param from 起始值（含）
     * @param to 终止值（含）
     * @return 生成的{@link IntStream}
     */
    public static IntStream intRangeClosed(int from, int to) {
        if (from <= to) {
            return rangeClosed(from, to);
        } else {
            return rangeClosed(to, from).map(i -> to - i + from);
        }
    }

    /**
     * 将一个迭代器转换为一个Stream
     *
     * @param iterator 输入的迭代器对象，不能为空
     * @param <T> 输入值的类型泛型
     * @return 生成的{@link Stream}
     */
    public static <T> Stream<T> toStream(Iterator<T> iterator) {
        checkNotNull(iterator);
        return stream(spliteratorUnknownSize(iterator, (NONNULL | IMMUTABLE | ORDERED)), false);
    }

    /**
     * 将一个可迭代对象转换为一个Stream
     *
     * @param iterable 输入的可迭代对象，不能为空
     * @param <T> 输入值的类型泛型
     * @return 生成的{@link Stream}
     */
    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        checkNotNull(iterable);
        if (iterable instanceof Collection) {
            // failfast
            try {
                Collection<T> collection = (Collection<T>) iterable;
                return stream(spliterator(collection, 0), false);
            } catch (Throwable e) {
                // do nothing
            }
        }
        return stream(spliteratorUnknownSize(iterable.iterator(), (NONNULL | IMMUTABLE | ORDERED)),
                false);
    }

    /**
     * 将一个Stream按一定的大小进行批次分组，返回一个按组的新的Stream
     *
     * @param stream 输入值的Stream
     * @param size 分组的大小
     * @param <T> 输入值的类型泛型
     * @return 生成的{@link Stream}
     */
    public static <T> Stream<List<T>> partition(Stream<T> stream, int size) {
        Iterable<List<T>> iterable = () -> Iterators.partition(stream.iterator(), size);
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
