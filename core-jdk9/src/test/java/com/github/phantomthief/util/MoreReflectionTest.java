package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreReflection.getCallerPlace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2018-06-13.
 */
class MoreReflectionTest {

    @Test
    void testCaller() {
        StackTraceElement callerPlace = MyTest.test3();
        assertNotNull(callerPlace);
        assertEquals("MoreReflectionTest.java", callerPlace.getFileName());

        callerPlace = MyTest2.test41();
        assertNotNull(callerPlace);
        assertEquals("MoreReflectionTest.java", callerPlace.getFileName());
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

        @Deprecated
        static StackTraceElement test3() {
            return getCallerPlace();
        }

        @Deprecated
        static StackTraceElement test4() {
            return getCallerPlace(MyTest.class);
        }
    }

    static class MyTest2 {
        static StackTraceElement test41() {
            return MyTest.test4();
        }
    }
}