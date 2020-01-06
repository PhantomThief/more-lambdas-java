package com.github.phantomthief.concurrent;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author w.vela
 * Created on 2019-11-08.
 */
class MoreFuturesTest2 {

    private final ListeningExecutorService executor = listeningDecorator(newFixedThreadPool(10));

    @Test
    void test() throws ExecutionException, InterruptedException {
        ListenableFuture<String> orig = executor.submit(() -> {
            sleepUninterruptibly(1, SECONDS);
            return "test";
        });
        boolean[] timeout = {false};
        ListenableFuture<String> timeout1 = new TimeoutListenableFuture<>(orig)
                .addTimeoutListener(e -> timeout[0] = true);
        assertThrows(TimeoutException.class, () -> timeout1.get(1, MILLISECONDS));
        assertTrue(timeout[0]);


        timeout[0] = false;
        ListenableFuture<String> timeout2 = MoreFutures.transform(timeout1, it -> it + "!", directExecutor());
        assertThrows(TimeoutException.class, () -> timeout2.get(1, MILLISECONDS));
        assertTrue(timeout[0]);
        assertEquals("test!", timeout2.get());
    }

    @Test
    void testTransformAsync() throws ExecutionException, InterruptedException {
        ListenableFuture<String> orig = executor.submit(() -> {
            sleepUninterruptibly(1, SECONDS);
            return "test";
        });
        boolean[] timeout = {false};
        ListenableFuture<String> timeout1 = new TimeoutListenableFuture<>(orig)
                .addTimeoutListener(e -> timeout[0] = true);
        assertThrows(TimeoutException.class, () -> timeout1.get(1, MILLISECONDS));
        assertTrue(timeout[0]);


        timeout[0] = false;
        ListenableFuture<String> timeout2 = MoreFutures
                .transformAsync(timeout1, it -> MoreExecutors.listeningDecorator(executor).submit(() -> it + "!"),
                        directExecutor());
        assertThrows(TimeoutException.class, () -> timeout2.get(1, MILLISECONDS));
        assertTrue(timeout[0]);
        assertEquals("test!", timeout2.get());
    }
}
