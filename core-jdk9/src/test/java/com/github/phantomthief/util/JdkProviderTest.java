package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreReflection.getCallerPlace;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2018-06-13.
 */
class JdkProviderTest {

    @Test
    void test() {
        MyTest.test();
        MyTest.test2();
    }

    @Test
    void testCaller8() {
        StackTraceElement callerPlace = MyTest.test81();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderTest.java", callerPlace.getFileName());

        callerPlace = MyTest2.test811();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderTest.java", callerPlace.getFileName());
    }

    @Test
    void testCaller9() {
        StackTraceElement callerPlace = MyTest.test91();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderTest.java", callerPlace.getFileName());

        callerPlace = MyTest2.test911();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderTest.java", callerPlace.getFileName());
    }

    @Test
    void testReflection8() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method test41 = MyTest2.class.getDeclaredMethod("test811");
        test41.setAccessible(true);
        StackTraceElement callerPlace = (StackTraceElement) test41.invoke(null);
        assertNotNull(callerPlace);
        assertEquals("JdkProviderTest.java", callerPlace.getFileName());
    }

    @Test
    void testReflection9() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method test41 = MyTest2.class.getDeclaredMethod("test911");
        test41.setAccessible(true);
        StackTraceElement callerPlace = (StackTraceElement) test41.invoke(null);
        assertNotNull(callerPlace);
        assertEquals("JdkProviderTest.java", callerPlace.getFileName());
    }

    static class MyTest {

        private static StackTraceProviderJdk9 jdk9 = new StackTraceProviderJdk9();
        private static StackTraceProviderJdk8 jdk8 = new StackTraceProviderJdk8();

        @Deprecated
        static void test() {
            System.err.println(getCallerPlace());
        }

        @Deprecated
        static void test2() {
            test();
        }

        @Deprecated
        static StackTraceElement test81() {
            return jdk8.getCallerPlace(StackTraceProviderJdk8.class);
        }

        @Deprecated
        static StackTraceElement test82() {
            return jdk8.getCallerPlace(MyTest.class);
        }

        @Deprecated
        static StackTraceElement test91() {
            return jdk9.getCallerPlace(StackTraceProviderJdk9.class);
        }

        @Deprecated
        static StackTraceElement test92() {
            return jdk9.getCallerPlace(MyTest.class);
        }
    }

    static class MyTest2 {
        static StackTraceElement test811() {
            return MyTest.test82();
        }
        static StackTraceElement test911() {
            return MyTest.test92();
        }
    }
}