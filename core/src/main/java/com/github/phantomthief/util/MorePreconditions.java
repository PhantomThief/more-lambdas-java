package com.github.phantomthief.util;

import static java.lang.String.format;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * MorePreconditions增强工具集合
 * <p>用于前置条件检查的工具集合</p>
 *
 * @author w.vela
 * Created on 2020-02-26.
 */
public final class MorePreconditions {

    /**
     * 检查一个条件，不符合时抛出提供器提供的异常
     * <p>与{@link com.google.common.base.Preconditions#checkArgument(boolean)}类似，但提供自定义的异常提供器</p>
     *
     * @param expression 守卫条件
     * @param exception 异常提供函数
     * @param <X> 抛出的异常类型泛型
     * @see com.google.common.base.Preconditions#checkArgument(boolean)
     */
    public static <X extends Throwable> void checkOrThrow(boolean expression, Supplier<X> exception) throws X {
        if (!expression) {
            throw exception.get();
        }
    }

    /**
     * 检查一个条件，不符合时抛出提供器提供的异常
     * <p>与{@link com.google.common.base.Preconditions#checkArgument(boolean, String, Object...)}类似，但提供自定义的异常提供器</p>
     *
     * @param expression 守卫条件
     * @param exception 异常提供函数
     * @param errorMessageTemplate 异常消息模板，使用{@link String#format(String, Object...)}}方式格式化
     * @param errorMessageArgs 异常消息
     * @param <X> 抛出的异常类型泛型
     * @see com.google.common.base.Preconditions#checkArgument(boolean)
     */
    public static <X extends Throwable> void checkOrThrow(boolean expression, Function<String, X> exception,
            String errorMessageTemplate, Object... errorMessageArgs) throws X {
        if (!expression) {
            throw exception.apply(format(errorMessageTemplate, errorMessageArgs));
        }
    }
}
