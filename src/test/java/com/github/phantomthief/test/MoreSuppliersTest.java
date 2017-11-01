package com.github.phantomthief.test;

import static com.github.phantomthief.util.MoreSuppliers.asyncLazy;
import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;

/**
 * @author w.vela
 */
class MoreSuppliersTest {

    @Test
    void test() {
        CloseableSupplier<Integer> lazy = lazy(() -> {
            System.out.println("start init...");
            sleepUninterruptibly(10, SECONDS);
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
        assertTrue(first != second);
    }

    @Test
    void test2() {
        CloseableSupplier<Integer> lazy = lazy(() -> new Random().nextInt(1000));
        Integer first = lazy.get();
        Integer second = lazy.get();
        assertTrue(first == second);
    }

    @Test
    void test3() {
        CloseableSupplier<Integer> lazy = lazy(() -> new Random().nextInt(1000));
        lazy.tryClose(t -> {
            fail("failed.");
        });
    }

    @Test
    void asyncTest() {
        Supplier<String> supplier = asyncLazy(() -> {
            System.out.println("initing...");
            sleepUninterruptibly(5, SECONDS);
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
    void asyncTestFailed() {
        int[] initTimes = { 0 };
        Supplier<String> supplier = asyncLazy(() -> {
            if (initTimes[0]++ <= 0) {
                System.out.println("failed");
                throw new RuntimeException("fail first time.");
            }
            System.out.println("initing...");
            sleepUninterruptibly(5, SECONDS);
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
