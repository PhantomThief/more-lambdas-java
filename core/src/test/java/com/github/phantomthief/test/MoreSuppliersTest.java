package com.github.phantomthief.test;

import static com.github.phantomthief.util.MoreSuppliers.asyncLazy;
import static com.github.phantomthief.util.MoreSuppliers.asyncLazyEx;
import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.github.phantomthief.util.MoreSuppliers.lazyEx;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.MoreSuppliers.AsyncSupplier;
import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;
import com.github.phantomthief.util.MoreSuppliers.CloseableThrowableSupplier;
import com.google.common.base.Stopwatch;

/**
 * @author w.vela
 */
class MoreSuppliersTest {

    @Test
    void test() {
        CloseableSupplier<Integer> lazy = lazy(() -> {
            System.out.println("start init...");
            sleepUninterruptibly(3, SECONDS);
            int nextInt = new Random().nextInt(1000);
            System.out.println("init result:" + nextInt);
            return nextInt;
        });
        new Thread(() -> {
            sleepUninterruptibly(1, SECONDS);
            lazy.tryClose(t -> {
                System.out.println("closeing t:" + t);
            });
        }).start();
        Integer first = lazy.get();
        System.out.println("first get:" + first);
        sleepUninterruptibly(500, MILLISECONDS);
        Integer second = lazy.get();
        System.out.println("second get:" + second);
        assertNotSame(first, second);
    }

    @Test
    void test2() {
        CloseableSupplier<Integer> lazy = lazy(() -> new Random().nextInt(1000));
        Integer first = lazy.get();
        Integer second = lazy.get();
        assertSame(first, second);
    }

    @Test
    void test3() {
        CloseableSupplier<Integer> lazy = lazy(() -> new Random().nextInt(1000));
        lazy.tryClose(t -> {
            fail("failed.");
        });
    }

    @Test
    void testEx() {
        CloseableThrowableSupplier<Integer, RuntimeException> lazy = lazyEx(() -> {
            System.out.println("start init...");
            sleepUninterruptibly(3, SECONDS);
            int nextInt = new Random().nextInt(1000);
            System.out.println("init result:" + nextInt);
            return nextInt;
        });
        new Thread(() -> {
            sleepUninterruptibly(1, SECONDS);
            lazy.tryClose(t -> {
                System.out.println("closeing t:" + t);
            });
        }).start();
        Integer first = lazy.get();
        System.out.println("first get:" + first);
        sleepUninterruptibly(500, MILLISECONDS);
        Integer second = lazy.get();
        System.out.println("second get:" + second);
        assertNotSame(first, second);
    }

    @Test
    void testEx2() {
        CloseableThrowableSupplier<Integer, RuntimeException> lazy = lazyEx(() -> new Random().nextInt(1000));
        Integer first = lazy.get();
        Integer second = lazy.get();
        assertSame(first, second);
    }

    @Test
    void testEx3() {
        CloseableThrowableSupplier<Integer, RuntimeException> lazy = lazyEx(() -> new Random().nextInt(1000));
        lazy.tryClose(t -> {
            fail("failed.");
        });
    }

    @Test
    void testExError() {
        CloseableThrowableSupplier<Integer, IOException> lazy = lazyEx(() -> {
            throw new IOException("io error");
        });
        Assertions.assertThrows(IOException.class, lazy::get);
    }

    @Test
    void asyncTest() {
        Supplier<String> supplier = asyncLazy(() -> {
            System.out.println("initing...");
            sleepUninterruptibly(3, SECONDS);
            System.out.println("inited.");
            return "test";
        });
        for (int i = 0; i < 10; i++) {
            String x = supplier.get();
            System.out.println(x);
            sleepUninterruptibly(1, SECONDS);
            if (i > 6) {
                assertEquals(x, "test");
            } else if (i < 3) {
                assertNull(x);
            }
        }
    }

    @Test
    void testAsyncWithWait() {
        Stopwatch sw = createStarted();
        AsyncSupplier<String> supplier = asyncLazyEx(() -> {
            System.out.println("initing...");
            sleepUninterruptibly(3, SECONDS);
            System.out.println("inited.");
            return "test";
        });
        assertNull(supplier.get(ofSeconds(1)));
        System.out.println("elapsed:" + sw);
        assertEquals(1, sw.elapsed(SECONDS));
        assertNull(supplier.get(ofSeconds(1)));
        System.out.println("elapsed:" + sw);
        assertEquals(1, sw.elapsed(SECONDS));
        assertNull(supplier.get(ofSeconds(2)));
        System.out.println("elapsed:" + sw);
        assertEquals(2, sw.elapsed(SECONDS));
        assertEquals("test", supplier.get(ofSeconds(4)));
        System.out.println("elapsed:" + sw);
        assertEquals(3, sw.elapsed(SECONDS));
        assertEquals("test", supplier.get(ofSeconds(4)));
        assertEquals(3, sw.elapsed(SECONDS));
    }

    @Test
    void asyncTestFailed() {
        int[] initTimes = {0};
        Supplier<String> supplier = asyncLazy(() -> {
            if (initTimes[0]++ <= 0) {
                System.out.println("failed");
                throw new RuntimeException("fail first time.");
            }
            System.out.println("initing...");
            sleepUninterruptibly(3, SECONDS);
            System.out.println("inited.");
            return "test";
        });
        for (int i = 0; i < 10; i++) {
            String x = supplier.get();
            System.out.println(x);
            sleepUninterruptibly(1, SECONDS);
            if (i > 6) {
                assertEquals(x, "test");
            } else if (i < 3) {
                assertNull(x);
            }
        }
    }
}
