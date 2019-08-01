package com.github.phantomthief.util;

import javax.annotation.Nullable;

/**
 * @author w.vela
 * Created on 2019-08-01.
 */
interface StackTraceProvider {

    @Nullable
    StackTraceElement getCallerPlace(Class<?> locationAwareClass);
}
