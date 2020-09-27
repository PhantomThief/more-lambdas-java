package com.github.phantomthief.util;

import static java.util.Collections.newSetFromMap;

import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * MorePredicates增强工具集合
 * <p>用于{@link Predicate}断言条件的工具集合</p>
 *
 * @author w.vela
 */
public final class MorePredicates {

    /**
     * 工具类，禁止实例化成对象
     */
    private MorePredicates() {
        throw new UnsupportedOperationException();
    }

    /**
     * 产生一个相反的断言条件，对一个输入的{@link Predicate}取not
     *
     * @param predicate 输入的条件对象
     * @param <T> 条件入参类型泛型
     * @return 新的断言条件
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return t -> !predicate.test(t);
    }

    /**
     * 当断言结果不成立时，执行指定的处理函数
     *
     * @param predicate 输入的条件对象
     * @param negateConsumer 条件不成立时执行的处理函数
     * @param <T> 条件入参类型泛型
     * @return 新的断言条件
     */
    public static <T> Predicate<T> applyOtherwise(Predicate<T> predicate,
            Consumer<T> negateConsumer) {
        return t -> {
            boolean result = predicate.test(t);
            if (!result) {
                negateConsumer.accept(t);
            }
            return result;
        };
    }

    /**
     * 唯一性检查的断言条件，相同标识的对象不能被重复出现
     *
     * @param mapper 映射函数，用于从输入对象获取唯一性检查的标识
     * @param <T> 条件入参类型泛型
     * @return 断言条件对象
     */
    public static <T> Predicate<T> distinctUsing(Function<T, Object> mapper) {
        return new Predicate<T>() {

            private final Set<Object> set = newSetFromMap(new ConcurrentHashMap<>());

            @Override
            public boolean test(T t) {
                return set.add(mapper.apply(t));
            }
        };
    }

    /**
     * 返回一个断言条件，用于检查当前元素是否等于指定的元素，或在指定的元素之后
     *
     * @param element 指定的元素对象
     * @param <T> 条件入参类型泛型
     * @return 断言条件对象
     */
    public static <T> Predicate<T> afterElement(T element) {
        return afterElement(element, true);
    }

    /**
     * 返回一个断言条件，用于检查当前元素是否等于指定的元素，或在指定的元素之后，可通过参数指定是否包括当前元素
     *
     * @param element 指定的元素对象
     * @param inclusive 是否包括指定的元素本身，如果是，则从指定的元素开始返回true，如果不是，则从指定元素序列之后返回true
     * @param <T> 条件入参类型泛型
     * @return 断言条件对象
     */
    public static <T> Predicate<T> afterElement(T element, boolean inclusive) {
        return after(e -> Objects.equals(element, e), inclusive);
    }

    /**
     * 返回一个断言条件，用于检查当前元素进行的测试，是否在入参条件测试成立时，或者在入参条件测试成立之后
     *
     * @param predicate 检查断言条件
     * @param <T> 条件入参类型泛型
     * @return 断言条件对象
     */
    public static <T> Predicate<T> after(Predicate<T> predicate) {
        return after(predicate, true);
    }

    /**
     * 返回一个断言条件，用于检查当前元素进行的测试，是否在入参条件测试成立时，或者在入参条件测试成立之后，可通过参数指定是否包括当前位置的元素
     *
     * @param predicate 检查断言条件
     * @param inclusive 是否包括指定的条件成立时刻，如果是，则从指定的元素的测试条件成立之时开始返回true，如果不是，则从指定元素的测试条件成立之后返回true
     * @param <T> 条件入参类型泛型
     * @return 断言条件对象
     */
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

    /**
     * 按照一定的随机概率决定的断言
     *
     * @param probability 随机概率，介于0.0-1.0之间，为0.0时表示永远不成立，为1.0时表示永远成立
     * @param <T> 条件入参类型泛型
     * @return 断言条件对象
     */
    public static <T> Predicate<T> probability(double probability) {
        return new Predicate<T>() {

            private final Random random = new Random();

            @Override
            public boolean test(T t) {
                return random.nextDouble() < probability;
            }
        };
    }
}
