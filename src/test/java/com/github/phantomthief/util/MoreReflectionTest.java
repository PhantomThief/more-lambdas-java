package com.github.phantomthief.util;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2018-06-13.
 */
class MoreReflectionTest {

    @Test
    void test() {
        MyTest.test();
        MyTest.test2();
    }

    static class MyTest {

        @Deprecated
        static void test() {
            MoreReflection.logDeprecated(System.err::println);
        }

        @Deprecated
        static void test2() {
            test();
        }
    }
}