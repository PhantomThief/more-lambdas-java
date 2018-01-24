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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;

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
            return rangeClosed(from, to);
        } else {
            return rangeClosed(to, from).map(i -> to - i + from);
        }
    }

    public static <T> Stream<T> toStream(Iterator<T> iterator) {
        checkNotNull(iterator);
        return stream(spliteratorUnknownSize(iterator, (NONNULL | IMMUTABLE | ORDERED)), false);
    }

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

    public static <T> Stream<List<T>> partition(Stream<T> stream, int size) {
        Iterable<List<T>> iterable = () -> Iterators.partition(stream.iterator(), size);
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T, X extends Throwable> void runParallel(ForkJoinPool pool, Stream<T> stream,
            ThrowableConsumer<Stream<T>, X> func) throws X {
        supplyParallel(pool, stream, it -> {
            func.accept(it);
            return null;
        });
    }

    public static <T, R, X extends Throwable> R supplyParallel(ForkJoinPool pool, Stream<T> stream,
            ThrowableFunction<Stream<T>, R, X> func) throws X {
        checkNotNull(pool);
        Throwable[] throwable = { null };
        ForkJoinTask<R> task = pool.submit(() -> {
            try {
                return func.apply(stream);
            } catch (Throwable e) {
                throwable[0] = e;
                return null;
            }
        });
        R r;
        try {
            r = task.get();
        } catch (ExecutionException | InterruptedException impossible) {
            throw new AssertionError(impossible);
        }
        if (throwable[0] != null) {
            //noinspection unchecked
            throw (X) throwable[0];
        }
        return r;
    }
}
