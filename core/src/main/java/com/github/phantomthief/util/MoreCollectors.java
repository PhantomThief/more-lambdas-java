package com.github.phantomthief.util;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
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
import java.util.stream.Stream;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongHashSet;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;

/**
 * MoreCollectors增强工具集合
 * <p>针对Java Stream收集器（{@link Collector}）的增强工具集，方便将Stream处理成指定类型的集合</p>
 *
 * @author w.vela
 */
public final class MoreCollectors {

    public static final Set<Collector.Characteristics> CH_ID = Collections
            .unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
    public static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();

    /**
     * 静态工具类，禁止初始化为对象
     */
    private MoreCollectors() {
        throw new UnsupportedOperationException();
    }

    /**
     * 将Stream转换为int类型列表的搜集器
     * <p>用于将Stream搜集为int类型的列表，使用{@link IntArrayList}作为{@link List}容器以降低的开销。</p>
     *
     * @return 搜集器对象
     */
    public static Collector<Integer, ?, IntArrayList> toIntList() {
        return new CollectorImpl<>(IntArrayList::new, IntArrayList::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, CH_ID);
    }

    /**
     * 将Stream转换为long类型列表的搜集器
     * <p>用于将Stream搜集为long类型的列表，使用{@link LongArrayList}作为{@link List}容器以降低的开销。</p>
     *
     * @return 搜集器对象
     */
    public static Collector<Long, ?, LongArrayList> toLongList() {
        return new CollectorImpl<>(LongArrayList::new, LongArrayList::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, CH_ID);
    }

    /**
     * 将Stream转换为int类型集合的搜集器
     * <p>用于将Stream搜集为int类型的集合，使用{@link IntHashSet}作为{@link Set}容器以降低的开销。</p>
     *
     * @return 搜集器对象
     */
    public static Collector<Integer, ?, IntHashSet> toIntSet() {
        return new CollectorImpl<>(IntHashSet::new, IntHashSet::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, CH_ID);
    }

    /**
     * 将Stream转换为long类型集合的搜集器
     * <p>用于将Stream搜集为long类型的集合，使用{@link LongHashSet}作为{@link Set}容器以降低的开销。</p>
     *
     * @return 搜集器对象
     */
    public static Collector<Long, ?, LongHashSet> toLongSet() {
        return new CollectorImpl<>(LongHashSet::new, LongHashSet::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, CH_ID);
    }

    /**
     * 将Stream转换为键为Integer的{@link Map}类型的集合的搜集器
     * <p>用于将Stream搜集为键为Integer的{@link Map}类型的集合，使用{@link IntObjectHashMap}作为{@link Map}容器以降低开销。</p>
     *
     * @param keyMapper {@link Map}的键映射器，将集合对象类型{@link T}映射为int类型
     * @param valueMapper {@link Map}的值映射器，将集合对象类型{@link T}映射为{@link U}类型
     * @param <T> 集合类型泛型
     * @param <K> 结果键类型，此处将使用固定的int类型泛型
     * @param <U> {@link Map}的结果值类型泛型
     * @return 搜集器对象
     */
    public static <T, K, U> Collector<T, IntObjectHashMap<U>, IntObjectHashMap<U>> toIntMap(
            ToIntFunction<? super T> keyMapper, Function<? super T, ? extends U> valueMapper) {
        BiConsumer<IntObjectHashMap<U>, T> accumulator = (map, element) -> map
                .put(keyMapper.applyAsInt(element), valueMapper.apply(element));
        return new CollectorImpl<>(IntObjectHashMap::new, accumulator, (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        }, CH_ID);
    }

    /**
     * 将{@link Entry}的Stream转换为{@link Map}类型的搜集器
     *
     * @param <K> 键类型泛型
     * @param <V> 结果值类型泛型
     * @return 搜集器对象
     */
    public static <K, V> Collector<Entry<K, V>, ?, Map<K, V>> toMap() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    /**
     * 已废弃：将Stream转换为元素类型为K的{@link HashMultiset}集合的搜集器
     *
     * @param elementMapper 元素映射器，将输入流的类型T映射为结果集合类型K
     * @param countMapper 元素个数映射器
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出集合的元素类型泛型
     * @return 搜集器对象
     */
    @Deprecated
    public static <T, K> Collector<T, ?, HashMultiset<K>> toMultiset(
            Function<? super T, ? extends K> elementMapper, ToIntFunction<? super T> countMapper) {
        BiConsumer<HashMultiset<K>, T> accumulator = (set, element) -> set
                .add(elementMapper.apply(element), countMapper.applyAsInt(element));
        BinaryOperator<HashMultiset<K>> finisher = (m1, m2) -> {
            m1.addAll(m2);
            return m1;
        };
        return new CollectorImpl<>(HashMultiset::create, accumulator, finisher, CH_ID);
    }

    /**
     * 已废弃：将Stream转换为{@link Multimap}类型的集合的搜集器
     * <p>
     * 当guava 21.0+时请使用 {@link com.google.common.collect.Multimaps#toMultimap(Function, Function, Supplier)}取代本方法
     * </p>
     *
     * @param keyMapper {@link Multimap}的键映射器，将集合对象类型{@link T}映射为{@link K}类型
     * @param valueMapper {@link Multimap}的值映射器，将集合对象类型{@link T}映射为{@link U}类型
     * @param supplier {@link Multimap}初始化器
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出{@link Multimap}的键类型泛型
     * @param <U> 输出{@link Multimap}的值类型泛型
     * @param <M> {@link Multimap}初始化器返回的类型
     * @return 搜集器对象
     */
    @Deprecated
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

    /**
     * 将Stream转换为{@link Map}类型的搜集器
     * <p>用于将Stream搜集为{@link Map}类型的Map，当输入流中存在键重复时会抛出{@link IllegalStateException}异常。</p>
     *
     * @param keyMapper {@link Map}的键映射器，将输入类型{@link T}映射为{@link K}类型
     * @param valueMapper {@link Map}的值映射器，将输入类型{@link T}映射为{@link U}类型
     * @param supplier {@link Map}的初始化提供器
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出{@link Map}的键类型泛型
     * @param <U> 输出{@link Map}的值类型泛型
     * @param <R> 初始化器返回的类型
     * @return 搜集器对象
     * @throws IllegalStateException 输入元素的键存在重复时会抛出此异常
     */
    public static <T, K, U, R extends Map<K, U>> Collector<T, ?, R> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper, Supplier<R> supplier) {
        return toMap(keyMapper, valueMapper, throwingMerger(), supplier);
    }

    /**
     * 将Stream转换为{@link Map}类型的搜集器
     *
     * @param keyMapper {@link Map}的键映射器，将输入类型{@link T}映射为{@link K}类型
     * @param valueMapper {@link Map}的值映射器，将输入类型{@link T}映射为{@link U}类型
     * @param mergeFunction {@link Map}相同Key合并的策略函数
     * @param mapSupplier {@link Map}的初始化提供器
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出{@link Map}的键类型泛型
     * @param <U> 输出{@link Map}的值类型泛型
     * @param <M> 初始化器返回的类型
     * @return 搜集器对象
     */
    private static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper, BinaryOperator<U> mergeFunction,
            Supplier<M> mapSupplier) {
        BiConsumer<M, T> accumulator = (map, element) -> map.merge(keyMapper.apply(element),
                valueMapper.apply(element), mergeFunction);
        return new CollectorImpl<>(mapSupplier, accumulator, mapMerger(mergeFunction), CH_ID);
    }

    /**
     * 将Stream转换为{@link Map}类型的搜集器，用于对元素进行分组，每个组一个{@link List}
     *
     * @param classifier 分类器，将元素映射为分组的Key
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出{@link Map}的键类型泛型
     * @return 搜集器对象
     */
    public static <T, K> Collector<T, ?, Map<K, List<T>>> groupingByAllowNullKey(
            Function<? super T, ? extends K> classifier) {
        return groupingByAllowNullKey(classifier, toList());
    }

    /**
     * 将Stream转换为{@link Map}类型的搜集器，用于对元素进行分组，支持定义每组的搜集方式，组Key支持null
     *
     * @param classifier 分类器，将元素映射为分组的Key
     * @param downstream 每组组内元素的搜集器
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出{@link Map}的键类型泛型
     * @param <A> 每组元素的累积类型泛型
     * @param <D> 组内聚合结果的泛型
     * @return 搜集器对象
     */
    public static <T, K, A, D> Collector<T, ?, Map<K, D>> groupingByAllowNullKey(
            Function<? super T, ? extends K> classifier,
            Collector<? super T, A, D> downstream) {
        return groupingByAllowNullKey(classifier, HashMap::new, downstream);
    }

    /**
     * 将Stream转换为{@link Map}类型的搜集器，用于对元素进行分组，支持定义每组的搜集方式，组Key支持null
     *
     * @param classifier 分类器，将元素映射为分组的Key
     * @param mapFactory {@link Map}提供器工厂，初始化Map对象
     * @param downstream 每组组内元素的搜集器
     * @param <T> 输入流的元素类型泛型
     * @param <K> 输出{@link Map}的键类型泛型
     * @param <D> 组内聚合结果的泛型
     * @param <A> 每组元素的累积类型泛型
     * @param <M> 初始化器返回的类型
     * @return 搜集器对象
     */
    public static <T, K, D, A, M extends Map<K, D>> Collector<T, ?, M> groupingByAllowNullKey(
            Function<? super T, ? extends K> classifier,
            Supplier<M> mapFactory,
            Collector<? super T, A, D> downstream) {
        Supplier<A> downstreamSupplier = downstream.supplier();
        BiConsumer<A, ? super T> downstreamAccumulator = downstream.accumulator();
        BiConsumer<Map<K, A>, T> accumulator = (m, t) -> {
            K key = classifier.apply(t);
            A container = m.computeIfAbsent(key, k -> downstreamSupplier.get());
            downstreamAccumulator.accept(container, t);
        };
        BinaryOperator<Map<K, A>> merger = mapMerger(downstream.combiner());
        @SuppressWarnings("unchecked")
        Supplier<Map<K, A>> mangledFactory = (Supplier<Map<K, A>>) mapFactory;

        if (downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            return new CollectorImpl<>(mangledFactory, accumulator, merger, CH_ID);
        } else {
            @SuppressWarnings("unchecked")
            Function<A, A> downstreamFinisher = (Function<A, A>) downstream.finisher();
            Function<Map<K, A>, M> finisher = intermediate -> {
                intermediate.replaceAll((k, v) -> downstreamFinisher.apply(v));
                @SuppressWarnings("unchecked")
                M castResult = (M) intermediate;
                return castResult;
            };
            return new CollectorImpl<>(mangledFactory, accumulator, merger, finisher, CH_NOID);
        }
    }

    /**
     * 类型强转
     *
     * @param <I> 输入类型泛型
     * @param <R> 输出类型泛型
     * @return 类型强转函数
     */
    @SuppressWarnings("unchecked")
    private static <I, R> Function<I, R> castingIdentity() {
        return i -> (R) i;
    }

    /**
     * 固定抛出异常的合并器，防止相同Key彼此覆盖
     *
     * @param <T> 值泛型
     * @return 合并器对象
     */
    public static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    /**
     * 用于Map合并的合并器
     *
     * @param mergeFunction 合并操作函数
     * @param <K> {@link Map}的键类型泛型
     * @param <V> {@link Map}的值类型泛型
     * @param <M> 操作值泛型
     * @return 合并器对象
     */
    private static <K, V, M extends Map<K, V>> BinaryOperator<M>
            mapMerger(BinaryOperator<V> mergeFunction) {
        return (m1, m2) -> {
            for (Map.Entry<K, V> e : m2.entrySet()) {
                m1.merge(e.getKey(), e.getValue(), mergeFunction);
            }
            return m1;
        };
    }

    /**
     * 组合搜集器，在转换最终结果前，使用指定的函数再次处理
     *
     * @param collector 搜集器对象
     * @param function 处理函数
     * @param <T> 输入流的元素类型泛型
     * @param <A> 每组元素的累积类型泛型
     * @param <R> 组内聚合结果的泛型
     * @param <S> 初始化器返回的类型
     * @return 返回一个新的搜集器
     */
    private static <T, A, R, S> Collector<T, ?, S> combine(Collector<T, A, R> collector,
            Function<? super R, ? extends S> function) {
        return Collector.of(collector.supplier(), collector.accumulator(), collector.combiner(),
                collector.finisher().andThen(function));
    }

    /**
     * 已废弃：将一个Stream连接上另一个Stream，返回一个新的Stream的搜集器
     * <p>尽量不要这么做，这可能会浪费一轮Stream迭代，可直接使用{@link Stream#concat(java.util.stream.Stream, java.util.stream.Stream)}</p>
     *
     * @param other 另一个Stream
     * @param <T> 输入流的元素类型泛型
     * @return 连接另一个Stream的搜集器
     */
    @Deprecated
    public static <T> Collector<T, ?, Stream<T>> concat(Stream<? extends T> other) {
        return combine(toList(), list -> Stream.concat(list.stream(), other));
    }

    /**
     * 将一个Stream连接上另一个元素，返回一个新的Stream的搜集器
     * <p>尽量不要这么做，这会可能会浪费一轮Stream迭代</p>
     *
     * @param element 另一个元素
     * @param <T> 输入流的元素类型泛型
     * @return 连接另一个元素的搜集器
     */
    @Deprecated
    public static <T> Collector<T, ?, Stream<T>> concat(T element) {
        return concat(of(element));
    }

    /**
     * {@code Collector}的简单实现，允许在外部传入一个Collector的各个函数部分
     *
     * @param <T> 输入流的元素类型泛型
     * @param <R> 输出结果类型泛型
     */
    public static class CollectorImpl<T, A, R> implements Collector<T, A, R> {

        private final Supplier<A> supplier;

        private final BiConsumer<A, T> accumulator;

        private final BinaryOperator<A> combiner;

        private final Function<A, R> finisher;

        private final Set<Characteristics> characteristics;

        /**
         * 构造{@code Collector}
         *
         * @param supplier 用于搜集的集合提供器
         * @param accumulator 中间结果计算
         * @param combiner 组合器
         * @param finisher 最终转换函数
         * @param characteristics 搜集器特征
         * @see Collector
         */
        public CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                BinaryOperator<A> combiner, Function<A, R> finisher,
                Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        /**
         * 构造{@code Collector}
         *
         * @param supplier 用于搜集的集合提供器
         * @param accumulator 中间结果计算
         * @param combiner 组合器
         * @param characteristics 搜集器特征
         * @see Collector
         */
        public CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator,
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
}
