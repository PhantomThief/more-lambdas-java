package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static com.google.common.reflect.Reflection.newProxy;
import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredStaticField;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import com.github.phantomthief.tuple.Tuple;
import com.github.phantomthief.tuple.TwoTuple;

/**
 * @author w.vela
 * Created on 2019-12-25.
 */
public class NameServiceUtils {

    static <T, R> R doInvoke(Object object, String method, T it) throws UnknownHostException {
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
    }

    static TwoTuple<Object, List<Object>> unwrapList(Object object) {
        if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            checkState(!list.isEmpty(), "empty nameService in jdk8");
            checkState(list.size() == 1, "multiple nameServices in jdk8");
            return Tuple.tuple(list.get(0), list);
        } else {
            throw new IllegalStateException("unknown jdk impl.");
        }
    }

    static <T> T adapterFromUnifiedNameService(UnifiedNameService unifiedNameService, Class<T> nameServiceInterface) {
        return newProxy(nameServiceInterface, (proxy, method, args) -> {
            String name = method.getName();
            if (name.equals("lookupAllHostAddr")) {
                return unifiedNameService.lookupAllHostAddr((String) args[0]);
            } else if (name.equals("getHostByAddr")) {
                return unifiedNameService.getHostByAddr((byte[]) args[0]);
            } else {
                return method.invoke(unifiedNameService, args);
            }
        });
    }

    static void setNameServiceJdk8(UnifiedNameService unifiedNameService, Object originalNameService0) {
        TwoTuple<Object, List<Object>> tuple = unwrapList(originalNameService0);
        Object originalNameService = tuple.getFirst();
        List<Object> list = tuple.getSecond();
        Class<?> nameServiceInterface = originalNameService.getClass().getInterfaces()[0];
        Object wrappedNameService = adapterFromUnifiedNameService(unifiedNameService, nameServiceInterface);
        list.set(0, wrappedNameService);
    }

    static void setNameServiceJdk9(UnifiedNameService unifiedNameService, Object originalNameService)
            throws IllegalAccessException {
        Class<?> nameServiceInterface = originalNameService.getClass().getInterfaces()[0];
        Object wrappedNameService = adapterFromUnifiedNameService(unifiedNameService, nameServiceInterface);
        writeStaticField(InetAddress.class, "nameService", wrappedNameService, true);
    }

    /**
     * @throws IllegalStateException 如果jdk8模式下已经使用SPI方式注入过多个实现时，或者开启JPMS时，权限不够
     */
    public static UnifiedNameService getCurrentNameService() {
        try {
            // jdk9
            Object nameService9 = readDeclaredStaticField(InetAddress.class, "nameService", true);
            if (nameService9 != null) {
                checkJdk9(nameService9);
                return new UnifiedNameServiceAdapter(nameService9);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // ignore
        }

        try {   // jdk8
            Object nameServices8 = readDeclaredStaticField(InetAddress.class, "nameServices", true);
            if (nameServices8 != null) {
                TwoTuple<Object, List<Object>> nameService8 = unwrapList(nameServices8);
                checkJdk8(nameService8.getFirst());
                return new UnifiedNameServiceAdapter(nameService8.getFirst());
            }
            throw new IllegalStateException("illegal jdk impl.");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkJdk8(Object nameService8) {
        try {
            String result = doInvoke(nameService8, "getHostByAddr", InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException e) {
            // ignore
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkJdk9(Object nameService9) {
        checkState(StringUtils.equals("java.net.InetAddress$PlatformNameService", nameService9.getClass().getName()),
                "unsupported jdk9+ impl.");
        try {
            String result = doInvoke(nameService9, "getHostByAddr", InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException e) {
            // ignore
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @throws IllegalStateException 如果jdk8模式下已经使用SPI方式注入过多个实现时，或者开启JPMS时，权限不够
     */
    public static void setNameService(UnifiedNameService unifiedNameService) {
        try {
            // jdk9
            Object nameService = readDeclaredStaticField(InetAddress.class, "nameService", true);
            if (nameService != null) {
                setNameServiceJdk9(unifiedNameService, nameService);
                return;
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // ignore
        }

        try {
            // jdk8
            Object nameService = readDeclaredStaticField(InetAddress.class, "nameServices", true);
            if (nameService != null) {
                setNameServiceJdk8(unifiedNameService, nameService);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class UnifiedNameServiceAdapter implements UnifiedNameService {

        private final Object object;

        UnifiedNameServiceAdapter(@Nonnull Object obj) {
            this.object = obj;
        }

        @Override
        public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
            return doInvoke(object, "lookupAllHostAddr", host);
        }

        @Override
        public String getHostByAddr(byte[] addr) throws UnknownHostException {
            return doInvoke(object, "getHostByAddr", addr);
        }
    }
}
