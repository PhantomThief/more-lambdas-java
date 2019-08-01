package com.github.phantomthief.util;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * @author w.vela
 * Created on 2019-06-18.
 */
public class ThrowableUtils {

    private static final Field MESSAGE_FIELD;

    static {
        try {
            MESSAGE_FIELD = Throwable.class.getDeclaredField("detailMessage");
            MESSAGE_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void changeThrowableMessage(Throwable throwable, Function<String, String> messageChangeFunc) {
        changeThrowableMessage(throwable, messageChangeFunc.apply(throwable.getMessage()));
    }

    public static void changeThrowableMessage(Throwable throwable, String message) {
        try {
            MESSAGE_FIELD.set(throwable, message);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
