package com.github.phantomthief.util;

import static com.google.common.reflect.Reflection.newProxy;

import java.util.function.Function;

/**
 * @author w.vela
 * Created on 16/5/7.
 */
public class ToStringHelper {

    public static <T> T wrapToString(Class<T> interfaceType, T obj,
            Function<T, String> toStringSupplier) {
        return newProxy(interfaceType, (proxy, method, args) -> {
            if (method.getName().equals("toString")) {
                return toStringSupplier.apply(obj);
            } else {
                return method.invoke(obj, args);
            }
        });
    }
}
