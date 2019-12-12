package com.github.phantomthief.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2018-06-13.
 */
class JdkProviderMultiClassTest {

    @Test
    void testCaller8() {
        StackTraceElement callerPlace = MyTest3.chain83();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderMultiClassTest.java", callerPlace.getFileName());

        callerPlace = MyTest6.chain86();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderMultiClassTest.java", callerPlace.getFileName());
    }

    @Test
    void testCaller9() {
        StackTraceElement callerPlace = MyTest3.chain93();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderMultiClassTest.java", callerPlace.getFileName());

        callerPlace = MyTest6.chain96();
        assertNotNull(callerPlace);
        assertEquals("JdkProviderMultiClassTest.java", callerPlace.getFileName());
    }

    static class MyTest {

        private static StackTraceProviderJdk9 jdk9 = new StackTraceProviderJdk9();
        private static StackTraceProviderJdk8 jdk8 = new StackTraceProviderJdk8();

        @Deprecated
        static StackTraceElement chain81() {
            return jdk8.getCallerPlace(MyTest.class, MyTest5.class);
        }

        @Deprecated
        static StackTraceElement chain91() {
            return jdk9.getCallerPlace(MyTest.class, MyTest5.class);
        }
    }

    static class MyTest2 {
        static StackTraceElement chain82() {
            return MyTest.chain81();
        }
        static StackTraceElement chain92() {
            return MyTest.chain91();
        }
    }

    static class MyTest3 {
        static StackTraceElement chain83() {
            return MyTest2.chain82();
        }
        static StackTraceElement chain93() {
            return MyTest2.chain92();
        }
    }

    static class MyTest4 {
        static StackTraceElement chain84() {
            return MyTest3.chain83();
        }
        static StackTraceElement chain94() {
            return MyTest3.chain93();
        }
    }

    static class MyTest5 {
        static StackTraceElement chain85() {
            return MyTest4.chain84();
        }
        static StackTraceElement chain95() {
            return MyTest4.chain94();
        }
    }

    static class MyTest6 {
        static StackTraceElement chain86() {
            return MyTest5.chain85();
        }
        static StackTraceElement chain96() {
            return MyTest5.chain95();
        }
    }
}