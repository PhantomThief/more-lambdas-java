package com.github.phantomthief.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 提供API方式注入NameService，兼容jdk8和jdk9+
 * 具体参考 {@link NameServiceUtils#getCurrentNameService()} 和 {@link NameServiceUtils#setNameService(UnifiedNameService)}
 *
 * @author w.vela
 * Created on 2019-12-25.
 */
public interface UnifiedNameService {

    InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException;

    String getHostByAddr(byte[] addr) throws UnknownHostException;
}

