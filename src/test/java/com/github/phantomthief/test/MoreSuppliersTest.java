/**
 * 
 */
package com.github.phantomthief.test;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;

import org.junit.Test;

import com.github.phantomthief.util.MoreSuppliers;
import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;

/**
 * @author w.vela
 */
public class MoreSuppliersTest {

    @Test
    public void test() throws Exception {
        CloseableSupplier<Integer> lazy = MoreSuppliers.lazy(() -> {
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
        CloseableSupplier<Integer> lazy = MoreSuppliers.lazy(() -> {
            int nextInt = new Random().nextInt(1000);
            return nextInt;
        });
        Integer first = lazy.get();
        Integer second = lazy.get();
        assertTrue(first == second);
    }

    @Test
    public void test3() throws Exception {
        CloseableSupplier<Integer> lazy = MoreSuppliers.lazy(() -> {
            int nextInt = new Random().nextInt(1000);
            return nextInt;
        });
        lazy.tryClose(t -> {
            fail();
        });
    }
}
