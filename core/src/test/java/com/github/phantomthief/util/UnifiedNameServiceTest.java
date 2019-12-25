package com.github.phantomthief.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2019-12-25.
 */
class UnifiedNameServiceTest {

    @Test
    void addNameService() throws IOException {
        boolean[] called = {false};
        UnifiedNameService.addNameService(new UnifiedNameService() {
            @Override
            public InetAddress[] lookupAllHostAddr(
                    ThrowableFunction<String, InetAddress[], UnknownHostException> origLookupAllHostAddr, String host)
                    throws UnknownHostException {
                InetAddress[] apply = origLookupAllHostAddr.apply(host);
                called[0] = true;
                logger.info("invoke lookupAllHostAddr:{}=>{}", host, apply);
                return apply;
            }

            @Override
            public String getHostByAddr(ThrowableFunction<byte[], String, UnknownHostException> origGetHostByAddr,
                    byte[] addr) throws UnknownHostException {
                String apply = origGetHostByAddr.apply(addr);
                called[0] = true;
                logger.info("invoke getHostByAddr:{}=>{}", addr, apply);
                return apply;
            }
        });
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("www.baidu.com", 443));
            assertTrue(called[0]);
        }
    }
}