package com.github.phantomthief.util;

import static com.github.phantomthief.util.ThrowableUtils.changeThrowableMessage;
import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author w.vela
 * Created on 2019-06-18.
 */
class ThrowableUtilsTest {

    private static final Logger logger = LoggerFactory.getLogger(ThrowableUtilsTest.class);

    @Test
    void test() {
        try {
            checkArgument(false, "myTest");
        } catch (IllegalArgumentException e) {
            changeThrowableMessage(e, it -> it + "!!!");
            assertEquals("myTest!!!", e.getMessage());
        }
    }
}