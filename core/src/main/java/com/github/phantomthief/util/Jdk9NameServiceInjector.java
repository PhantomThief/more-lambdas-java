package com.github.phantomthief.util;

import static com.github.phantomthief.util.NameServiceUtils.wrapAsFunction;
import static com.google.common.reflect.Reflection.newProxy;
import static org.apache.commons.lang3.reflect.FieldUtils.writeStaticField;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author w.vela
 * Created on 2019-12-25.
 */
class Jdk9NameServiceInjector {

    static void addNameService(UnifiedNameService unifiedNameService, Object originalNameService)
            throws IllegalAccessException {
        Class<?> nameServiceInterface = originalNameService.getClass().getInterfaces()[0];
        ThrowableFunction<String, InetAddress[], UnknownHostException> lookupAllHostAddr =
                wrapAsFunction(originalNameService, "lookupAllHostAddr");
        ThrowableFunction<byte[], String, UnknownHostException> getHostByAddr =
                wrapAsFunction(originalNameService, "getHostByAddr");
        Object wrappedNameService = newProxy(nameServiceInterface, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if (name.equals("lookupAllHostAddr")) {
                    return unifiedNameService.lookupAllHostAddr(lookupAllHostAddr, (String) args[0]);
                } else if (name.equals("getHostByAddr")) {
                    return unifiedNameService.getHostByAddr(getHostByAddr, (byte[]) args[0]);
                } else {
                    return method.invoke(unifiedNameService, args);
                }
            }
        });
        writeStaticField(InetAddress.class, "nameService", wrappedNameService, true);
    }
}
