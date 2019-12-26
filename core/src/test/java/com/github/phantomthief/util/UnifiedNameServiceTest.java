package com.github.phantomthief.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void test() throws IOException {
        boolean[] called = {false};
        UnifiedNameService previousNameService = NameServiceUtils.getCurrentNameService();
        assertNotNull(previousNameService);
        NameServiceUtils.setNameService(new UnifiedNameService() {
            @Override
            public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
                InetAddress[] apply = previousNameService.lookupAllHostAddr(host);
                called[0] = true;
                NameServiceUtils.logger.info("invoke lookupAllHostAddr:{}=>{}", host, apply);
                return apply;
            }

            @Override
            public String getHostByAddr(byte[] addr) throws UnknownHostException {
                String apply = previousNameService.getHostByAddr(addr);
                called[0] = true;
                NameServiceUtils.logger.info("invoke getHostByAddr:{}=>{}", addr, apply);
                return apply;
            }
        });
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("www.baidu.com", 443));
            assertTrue(called[0]);
        }
    }
}