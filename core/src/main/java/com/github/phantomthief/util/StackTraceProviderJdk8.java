package com.github.phantomthief.util;

import javax.annotation.Nullable;

/**
 * @author w.vela
 * Created on 2019-08-01.
 */
class StackTraceProviderJdk8 implements StackTraceProvider {

    @Nullable
    @Override
    public StackTraceElement getCallerPlace(Class<?> locationAwareClass) {
        String locationAwareClassName = locationAwareClass.getName();

        boolean afterSelf = false;
        boolean afterDeprecated = false;
        String deprecatedClass = null;
        // 之所以用异常获取而不是Thread.currentThread().getStackTrace()，是因为它内部实现其实也是判断当前线程了
        for (StackTraceElement stack : (new Exception()).getStackTrace()) {
            String stackClassName = stack.getClassName();
            if (stackClassName.equals(locationAwareClassName)) {
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
                return stack;
            }
        }
        return null;
    }
}
