package com.github.phantomthief.util;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.stream;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author w.vela <wangtianzhou@kuaishou.com>
 * Created on 2017-02-24.
 */
public class DeadlineCheckerTest {

    private static DeadlineChecker helper;

    @BeforeClass
    public static void setup() {
        helper = DeadlineChecker.deadlineWithMinTicker(ofMillis(10));
    }

    @Test
    public void test() {
        System.out.println("start.");
        helper.runWithDeadline(() -> { //
            sleepUninterruptibly(5, SECONDS);
        }, ofSeconds(2), t -> { //
            System.err.println("slow run found. thread:" + t + ", stack:"
                    + stream(t.getStackTrace()).map(StackTraceElement::toString) //
                            .collect(joining("\n")));
        });
        System.out.println("end.");

        assertTrue(helper.RUNNING.isEmpty());
    }

    @Test
    public void testMultiThread() {
        ExecutorService executor = newFixedThreadPool(10);
        Set<Integer> slowed = synchronizedSet(new HashSet<>());
        for (int i = 0; i < 10; i++) {
            int j = i;
            executor.execute(() -> { //
                assertEquals(helper.supplyWithDeadline(() -> {
                    if (j < 5) {
                        sleepUninterruptibly(2, SECONDS);
                    } else if (j > 8) {
                        throw new RuntimeException("" + j);
                    }
                    return j;
                }, ofSeconds(1), t -> {
                    slowed.add(j);
                    System.err.println("slow found:" + j + ", thread:" + t);
                }), Integer.valueOf(j));
            });
        }
        shutdownAndAwaitTermination(executor, 1, DAYS);
        assertTrue(!slowed.isEmpty());
        slowed.forEach(slow -> assertTrue(slow < 5));
        assertTrue(helper.RUNNING.isEmpty());
    }

    @Test
    public void testLoop() {
        System.out.println("start.");
        helper.runWithDeadline(() -> { //
            helper.runWithDeadline(() -> { //
                // inner run
                sleepUninterruptibly(5, SECONDS);
            }, ofSeconds(1), t -> System.err.println("inner slow:" + t));
            // outer run
            sleepUninterruptibly(5, SECONDS);
        }, ofSeconds(2), t -> { //
            System.err.println("slow run found. thread:" + t);
        });
        System.out.println("end.");

        assertTrue(helper.RUNNING.isEmpty());
    }
}