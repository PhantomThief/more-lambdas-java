/**
 * 
 */
package com.github.phantomthief.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongHashSet;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;

/**
 * @author w.vela <vela@longbeach-inc.com>
 *
 * @date 2014年6月18日 下午5:30:34
 */
public final class MoreCollectors {

    static final Set<Collector.Characteristics> CH_ID = Collections
            .unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));

    public static Collector<Integer, ?, IntArrayList> toIntList() {
        return new CollectorImpl<Integer, IntArrayList, IntArrayList>(IntArrayList::new,
                IntArrayList::add, (left, right) -> {
                    left.addAll(right);
                    return left;
                } , CH_ID);
    }

    public static Collector<Long, ?, LongArrayList> toLongList() {
        return new CollectorImpl<Long, LongArrayList, LongArrayList>(LongArrayList::new,
                LongArrayList::add, (left, right) -> {
                    left.addAll(right);
                    return left;
                } , CH_ID);
    }

    public static Collector<Integer, ?, IntHashSet> toIntSet() {
        return new CollectorImpl<Integer, IntHashSet, IntHashSet>(IntHashSet::new, IntHashSet::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                } , CH_ID);
    }

    public static Collector<Long, ?, LongHashSet> toLongSet() {
        return new CollectorImpl<Long, LongHashSet, LongHashSet>(LongHashSet::new, LongHashSet::add,
                (left, right) -> {
                    left.addAll(right);
                    return left;
                } , CH_ID);
    }

    public static <T, R extends List<T>> Collector<T, ?, R> toList(Supplier<R> supplier) {
        return new CollectorImpl<>(supplier, List::add, (left, right) -> {
            left.addAll(right);
            return left;
        } , CH_ID);
    }

    public static <T, R extends Set<T>> Collector<T, ?, R> toSet(Supplier<R> supplier) {
        return new CollectorImpl<>(supplier, Set::add, (left, right) -> {
            left.addAll(right);
            return left;
        } , CH_ID);
    }

    public static <T, K, U> Collector<T, IntObjectHashMap<U>, IntObjectHashMap<U>> toIntMap(
            ToIntFunction<? super T> keyMapper, Function<? super T, ? extends U> valueMapper) {
        BiConsumer<IntObjectHashMap<U>, T> accumulator = (map, element) -> map
                .put(keyMapper.applyAsInt(element), valueMapper.apply(element));
        return new CollectorImpl<>(IntObjectHashMap::new, accumulator, (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        } , CH_ID);
    }

    public static <K, V> Collector<Entry<K, V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <T, K> Collector<T, ?, HashMultiset<K>> toMultiset(
            Function<? super T, ? extends K> elementMapper, ToIntFunction<? super T> countMapper) {
        BiConsumer<HashMultiset<K>, T> accumulator = (set, element) -> set
                .add(elementMapper.apply(element), countMapper.applyAsInt(element));
        BinaryOperator<HashMultiset<K>> finisher = (m1, m2) -> {
            m1.addAll(m2);
            return m1;
        };
        return new CollectorImpl<T, HashMultiset<K>, HashMultiset<K>>(HashMultiset::create,
                accumulator, finisher, CH_ID);
    }

    public static <T, K, U, M extends Multimap<K, U>> Collector<T, ?, M> toMultimap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper, Supplier<M> supplier) {
        BiConsumer<M, T> accumulator = (multimap, element) -> multimap.put(keyMapper.apply(element),
                valueMapper.apply(element));
        BinaryOperator<M> finisher = (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        };
        return new CollectorImpl<>(supplier, accumulator, finisher, CH_ID);
    }

    public static <T, K, U, R extends Map<K, U>> Collector<T, ?, R> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper, Supplier<R> supplier) {
        return toMap(keyMapper, valueMapper, throwingMerger(), supplier);
    }

    private static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction,
            Supplier<M> mapSupplier) {
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element),
                valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(mapSupplier, accumulator, mapMerger(mergeFunction), CH_ID);
    }

    /**
     * Simple implementation class for {@code Collector}.
     *
     * @param <T> the type of elements to be collected
     * @param <R> the type of the result
     */
    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {

        private final Supplier<A> supplier;

        private final BiConsumer<A, T> accumulator;

        private final BinaryOperator<A> combiner;

        private final Function<A, R> finisher;

        private final Set<Characteristics> characteristics;

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                BinaryOperator<A> combiner, Function<A, R> finisher,
                Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                BinaryOperator<A> combiner, Set<Characteristics> characteristics) {
            this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
    }

    @SuppressWarnings("unchecked")
    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }

    private static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    private static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(
            BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
                m1.merge(e.getKey(), e.getValue(), mergeFunction);
            }
            return m1;
        };
    }

    public static final void main(String args[]) {
        Collection<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        IntArrayList collect = list.stream().collect(MoreCollectors.toIntList());
        System.out.println(collect);
        IntHashSet collectSet = list.stream().collect(MoreCollectors.toIntSet());
        System.out.println(collectSet);
        LinkedList<Integer> linkedList = list.stream()
                .collect(MoreCollectors.toList(LinkedList::new));
        System.out.println(linkedList);
        LinkedHashMap<Integer, Integer> map = list.stream().collect(
                MoreCollectors.toMap(Function.identity(), Function.identity(), LinkedHashMap::new));
        System.out.println(map);
    }
}
