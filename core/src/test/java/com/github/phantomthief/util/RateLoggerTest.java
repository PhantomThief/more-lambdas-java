package com.github.phantomthief.util;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author w.vela
 * Created on 2020-08-31.
 */
class RateLoggerTest {

    @Test
    void test1() {
        Logger logger = LoggerFactory.getLogger("test");
        RateLogger rateLogger = RateLogger.rateLogger(logger);
        int[] toStringCalled = {0};
        Object obj = new Object() {
            @Override
            public String toString() {
                toStringCalled[0]++;
                return super.toString();
            }
        };
        for (int i = 0; i < 10; i++) {
            rateLogger.info("test1:{} (EXPECTED ONLY TWICE)", obj);
            rateLogger.info("test2:{} (EXPECTED ONLY TWICE)", obj);
            sleepUninterruptibly(200, MILLISECONDS);
        }
        assertEquals(2, toStringCalled[0]);
    }

    @Test
    void test2() {
        Logger logger = LoggerFactory.getLogger("test2");
        RateLogger rateLogger = RateLogger.perFormatStringRateLogger(logger);
        int[] toStringCalled = {0};
        Object obj = new Object() {
            @Override
            public String toString() {
                toStringCalled[0]++;
                return super.toString();
            }
        };
        for (int i = 0; i < 10; i++) {
            rateLogger.info("test1:{} (EXPECTED SHOW 4 TIMES)", obj);
            rateLogger.info("test2:{} (EXPECTED SHOW 4 TIMES)", obj);
            sleepUninterruptibly(200, MILLISECONDS);
        }
        assertEquals(4, toStringCalled[0]);
    }
}