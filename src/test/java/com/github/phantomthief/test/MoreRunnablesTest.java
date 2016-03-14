package com.github.phantomthief.test;

import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.github.phantomthief.util.MoreRunnables;

/**
 * @author w.vela
 * Created on 16/3/14.
 */
public class MoreRunnablesTest {

    @Test
    public void test() {
        AtomicBoolean runned = new AtomicBoolean(false);
        Runnable runOnce = MoreRunnables.runOnce(() -> {
            System.out.println("check run.");
            if (runned.get()) {
                fail();
            } else {
                runned.set(true);
            }
            System.out.println("run");
        });
        for (int i = 0; i < 10; i++) {
            runOnce.run();
        }
    }
}
