package com.github.phantomthief.util;

import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicates;

/**
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
