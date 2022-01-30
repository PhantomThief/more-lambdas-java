package com.github.phantomthief.test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.MoreSuppliers;
import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;

/**
 * @author w.vela
 * Created on 2022-01-30.
 */
class MoreSuppliersConcurrentTest {

    @Test
    void test() throws InterruptedException {
        CloseableSupplier<String> supplier = MoreSuppliers.lazy(this::init, true);
        AtomicBoolean term = new AtomicBoolean();
        LongAdder succ = new LongAdder();
        LongAdder failed = new LongAdder();
        new Thread(() -> {
            while(!term.get()) {
                try {
                    supplier.tryClose(it -> {
                        MILLISECONDS.sleep(1);
                    });
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        }).start();
        for (int i = 0; i < 100; i++) {
            new Thread(() -> {
                while (!term.get()) {
                    String s = supplier.get();
                    if (s == null) {
                        failed.increment();
                    } else {
                        succ.increment();
                    }
                }
            }).start();
        }
        for (int i = 0; i < 30; i++) {
            System.out.println("waiting:" + i + ", current:" + succ.sum() + ", " + failed.sum());
            SECONDS.sleep(1);
            if (failed.sum() > 0) {
                break;
            }
        }
        term.set(true);
        assertTrue(failed.sum() == 0);
    }

    private String init() {
        return System.currentTimeMillis() + "";
    }
}
