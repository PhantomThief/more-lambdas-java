package com.github.phantomthief.util;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
        for (int i = 0; i < 10; i++) {
            rateLogger.info("test1:{}", i);
            rateLogger.info("test2:{}", i);
            sleepUninterruptibly(200, MILLISECONDS);
        }
    }

    @Test
    void test2() {
        Logger logger = LoggerFactory.getLogger("test2");
        RateLogger rateLogger = RateLogger.perFormatStringRateLogger(logger);
        for (int i = 0; i < 10; i++) {
            rateLogger.info("test1:{}", i);
            rateLogger.info("test2:{}", i);
            sleepUninterruptibly(200, MILLISECONDS);
        }
    }
}