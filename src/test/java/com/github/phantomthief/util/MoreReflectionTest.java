package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreReflection.getCallerPlace;

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

    @Test
    void testKotlinCaller() {
        KotlinCaller.test1();
    }

    static class MyTest {

        @Deprecated
        static void test() {
            System.err.println(getCallerPlace());
        }

        @Deprecated
        static void test2() {
            test();
        }
    }
}