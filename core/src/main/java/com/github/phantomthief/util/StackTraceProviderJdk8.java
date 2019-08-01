package com.github.phantomthief.util;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Thread.currentThread;

import java.util.List;

import javax.annotation.Nullable;

/**
 * @author w.vela
 * Created on 2019-08-01.
 */
class StackTraceProviderJdk8 implements StackTraceProvider {

    @Nullable
    @Override
    public StackTraceElement getCallerPlace(Class<?> locationAwareClass) {
        List<StackTraceElement> stackTrace = newArrayList(currentThread().getStackTrace());
        boolean afterSelf = false;
        boolean afterDeprecated = false;
        String deprecatedClass = null;
        for (StackTraceElement stack : stackTrace) {
            if (stack.getClassName().equals(locationAwareClass.getName())) {
                afterSelf = true;
                continue;
            }
            if (afterSelf) {
                if (deprecatedClass == null
                        && !stack.getClassName().equals(locationAwareClass.getName())) {
                    deprecatedClass = stack.getClassName();
                }
            }
            if (stack.getClassName().equals(deprecatedClass)) {
                afterDeprecated = true;
                continue;
            }
            if (afterDeprecated) {
                return stack;
            }
        }
        return null;
    }
}
