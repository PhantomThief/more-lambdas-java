package com.github.phantomthief.util;

import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredStaticField;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供API方式注入NameService，兼容jdk8和jdk9+
 *
 * @author w.vela
 * Created on 2019-12-25.
 */
public interface UnifiedNameService {

    Logger logger = LoggerFactory.getLogger(UnifiedNameService.class);

    /**
     * @throws IllegalStateException 如果jdk8模式下已经使用SPI方式注入过多个实现时，或者开启JPMS时，权限不够
     */
    static void addNameService(UnifiedNameService unifiedNameService) {
        try {
            // jdk9
            Object nameService = readDeclaredStaticField(InetAddress.class, "nameService", true);
            if (nameService != null) {
                Jdk9NameServiceInjector.addNameService(unifiedNameService, nameService);
                logger.info("add UnifiedNameService to jdk9+ successfully.");
                return;
            }

            // jdk8
            nameService = readDeclaredStaticField(InetAddress.class, "nameServices", true);
            if (nameService != null) {
                Jdk8NameServiceInjector.addNameService(unifiedNameService, nameService);
                logger.info("add UnifiedNameService to jdk8 successfully.");
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    InetAddress[] lookupAllHostAddr(ThrowableFunction<String, InetAddress[], UnknownHostException> origLookupAllHostAddr,
            String host) throws UnknownHostException;

    String getHostByAddr(ThrowableFunction<byte[], String, UnknownHostException> origGetHostByAddr, byte[] addr)
            throws UnknownHostException;
}

