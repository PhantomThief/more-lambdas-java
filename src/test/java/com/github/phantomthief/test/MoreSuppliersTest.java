/**
 * 
 */
package com.github.phantomthief.test;

import static com.github.phantomthief.util.MoreSuppliers.asyncLazy;
import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.function.Supplier;

import org.junit.Test;

import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;

/**
 * @author w.vela
 */
public class MoreSuppliersTest {

    @Test
    public void test() throws Exception {
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
    public void test2() throws Exception {
        CloseableSupplier<Integer> lazy = lazy(() -> {
            int nextInt = new Random().nextInt(1000);
            return nextInt;
        });
        Integer first = lazy.get();
        Integer second = lazy.get();
        assertTrue(first == second);
    }

    @Test
    public void test3() throws Exception {
        CloseableSupplier<Integer> lazy = lazy(() -> {
            int nextInt = new Random().nextInt(1000);
            return nextInt;
        });
        lazy.tryClose(t -> {
            fail();
        });
    }

    @Test
    public void asyncTest() {
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
    public void asyncTestFailed() {
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
