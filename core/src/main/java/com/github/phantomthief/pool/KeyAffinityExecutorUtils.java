package com.github.phantomthief.pool;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.github.phantomthief.pool.impl.DynamicCapacityLinkedBlockingQueue;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author w.vela
 * Created on 2020-08-17.
 */
public class KeyAffinityExecutorUtils {

    public static final int RANDOM_THRESHOLD = 20;

    static Supplier<ExecutorService> executor(String threadName, IntSupplier queueBufferSize) {
        return new Supplier<ExecutorService>() {

            private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(threadName)
                    .build();

            @Override
            public ExecutorService get() {
                BlockingQueue<Runnable> queue = new DynamicCapacityLinkedBlockingQueue<Runnable>(queueBufferSize) {
                    @Override
                    public boolean offer(Runnable o) {
                        try {
                            put(o);
                            return true;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        return false;
                    }
                };
                return new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS, queue, threadFactory);
            }
        };
    }

    static Supplier<ExecutorService> executor(String threadName, int queueBufferSize) {
        return new Supplier<ExecutorService>() {

            private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(threadName)
                    .build();

            @Override
            public ExecutorService get() {
                LinkedBlockingQueue<Runnable> queue;
                if (queueBufferSize > 0) {
                    queue = new LinkedBlockingQueue<Runnable>(queueBufferSize) {

                        @Override
                        public boolean offer(Runnable e) {
                            try {
                                put(e);
                                return true;
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            return false;
                        }
                    };
                } else {
                    queue = new LinkedBlockingQueue<>();
                }
                return new ThreadPoolExecutor(1, 1, 0L, MILLISECONDS, queue, threadFactory);
            }
        };
    }
}
