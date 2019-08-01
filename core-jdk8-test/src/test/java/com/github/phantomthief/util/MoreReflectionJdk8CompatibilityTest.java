package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreReflection.getStackTraceProvider;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2019-08-01.
 */
class MoreReflectionJdk8CompatibilityTest {

    @Test
    void test() {
        StackTraceProvider stackTraceProvider = getStackTraceProvider();
        assertNotNull(stackTraceProvider);
        assertSame(StackTraceProviderJdk8.class, stackTraceProvider.getClass());
    }
}
