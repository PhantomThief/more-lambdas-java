package com.github.phantomthief.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * @author w.vela
 * Created on 2019-08-01.
 */
class StackTraceProviderJdk8 implements StackTraceProvider {

    static final String[] REFLECTION_PREFIXES = {"sun.reflect.", "java.lang.reflect.", "jdk.internal.reflect."};

    @Nullable
    @Override
    public StackTraceElement getCallerPlace(Predicate<String> locationAwareClassChecker, Predicate<String> ignore) {
        boolean afterSelf = false;
        boolean afterDeprecated = false;
        String deprecatedClass = null;
        // 之所以用异常获取而不是Thread.currentThread().getStackTrace()，是因为它内部实现其实也是判断当前线程了
        for (StackTraceElement stack : (new Exception()).getStackTrace()) {
            String stackClassName = stack.getClassName();
            if (isReflection(stackClassName)) {
                continue;
            }
            if (locationAwareClassChecker.test(stackClassName)) {
                afterSelf = true;
                continue;
            }
            if (afterSelf) {
                if (deprecatedClass == null) {
                    deprecatedClass = stackClassName;
                }
            }
            if (stackClassName.equals(deprecatedClass)) {
                afterDeprecated = true;
                continue;
            }
            if (afterDeprecated) {
                if (ignore.test(stackClassName)) {
                    continue;
                }
                return stack;
            }
        }
        return null;
    }

    /**
     * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
     * at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     * at java.lang.reflect.Method.invoke(Method.java:498)
     */
    private boolean isReflection(String stackClassName) {
        return StringUtils.startsWithAny(stackClassName, REFLECTION_PREFIXES);
    }

    private Set<String> toString(Class<?>[] locationAwareClasses) {
        int length = locationAwareClasses.length;
        if (length == 1) { // optimize
            return Collections.singleton(locationAwareClasses[0].getName());
        }
        Set<String> result = new HashSet<>(length);
        for (Class<?> locationAwareClass : locationAwareClasses) {
            result.add(locationAwareClass.getName());
        }
        return result;
    }
}
