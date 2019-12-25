package com.github.phantomthief.util;

import static com.google.common.base.Throwables.throwIfUnchecked;

import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * @author w.vela
 * Created on 2019-12-25.
 */
class NameServiceUtils {

    static <T, R> ThrowableFunction<T, R, UnknownHostException> wrapAsFunction(Object object, String method) {
        return it -> {
            try {
                //noinspection unchecked
                return (R) MethodUtils.invokeMethod(object, true, method, it);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof UnknownHostException) {
                    throw (UnknownHostException) cause;
                }
                throwIfUnchecked(cause);
                throw new AssertionError(cause);
            }
        };
    }
}
