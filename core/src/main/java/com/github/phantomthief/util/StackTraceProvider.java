package com.github.phantomthief.util;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicates;

/**
 * 调用栈提供器接口
 * <p>用于获得当前调用点向上查找的调用栈位置，通常应用在需要获得方法调用位置的场景。默认包括JDK8/JDK9的两个实现，JDK9的实现更高效。</p>
 *
 * @author w.vela
 * Created on 2019-08-01.
 */
interface StackTraceProvider {

    /**
     * 返回 {@param locationAwareClassChecker} 最接近调用方方向第二个元素（跳过一个）
     */
    @Nullable
    StackTraceElement getCallerPlace(Predicate<String> locationAwareClassChecker, Predicate<String> ignore);

    @Nullable
    default StackTraceElement getCallerPlace(Class<?> locationAwareClassChecker) {
        String name = locationAwareClassChecker.getName();
        return getCallerPlace(name::equals, Predicates.alwaysFalse());
    }
}
