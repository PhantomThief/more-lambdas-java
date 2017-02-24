package com.github.phantomthief.util;

import static com.github.phantomthief.tuple.Tuple.tuple;
import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.github.phantomthief.tuple.TwoTuple;
import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * When a task is running.
 * NORMALLY checking if it's slow:
 *  1. create a new thread to check if it run out of deadline by calling Future.get(timeout)
 *  2. when the task was finished, check if it's slow with a stopwatch.
 *
 * while there are some problems:
 *  if a task didn't end(or spend too much time), it could never found slow without spawn a thread,
 *  or found the deadline exceeded on time.
 *
 * This helper make another solution:
 *  Gather running tasks in a container and check if there is a deadline occurred periodically.
 *
 * @author w.vela
 * Created on 2017-02-24.
 */
public class DeadlineChecker implements AutoCloseable {

    private static final Logger logger = getLogger(DeadlineChecker.class);

    final Map<Thread, List<DeadlineInfo>> RUNNING = new ConcurrentHashMap<>();

    private final CloseableSupplier<TwoTuple<ExecutorService, ScheduledFuture<?>>> scheduler;

    private DeadlineChecker(long ticker) {
        this.scheduler = lazy(() -> {
            ScheduledExecutorService executor = newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder() //
                            .setNameFormat("deadline-helper-%d") //
                            .build());
            return tuple(executor, executor.scheduleAtFixedRate(this::checkDeadline, ticker, ticker,
                    MILLISECONDS));
        });
    }

    public static DeadlineChecker deadlineWithMinTicker(Duration minTicker) {
        long ticker = minTicker.toMillis();
        checkArgument(ticker > 0, "invalid min ticker, it must be larger than 1ms.");
        return new DeadlineChecker(ticker);
    }

    /**
     * @param runnable main task which would be run on caller's thread
     * @param deadlineExceeded triggered in another thread when the main task's deadline exceeded.
     *                         the consumer would be running in ticker's thread, so it should be short and simple.
     */
    public <X extends Throwable> void runWithDeadline(ThrowableRunnable<X> runnable,
            Duration deadline, Consumer<Thread> deadlineExceeded) throws X {
        supplyWithDeadline(() -> {
            runnable.run();
            return null;
        }, deadline, deadlineExceeded);
    }

    /**
     * @param supplier main task which would be run on caller's thread
     * @param deadlineExceeded triggered in another thread when the main task's deadline exceeded.
     *                         the consumer would be running in ticker's thread, so it should be short and simple.
     */
    public <T, X extends Throwable> T supplyWithDeadline(ThrowableSupplier<T, X> supplier,
            Duration deadline, Consumer<Thread> deadlineExceeded) throws X {
        scheduler.get();
        Thread thread = currentThread();
        DeadlineInfo deadlineInfo = new DeadlineInfo(deadline.toMillis(),
                thread, deadlineExceeded);
        RUNNING.compute(thread, (t, deadlines) -> {
            if (deadlines == null) {
                deadlines = new CopyOnWriteArrayList<>();
            }
            deadlines.add(deadlineInfo);
            return deadlines;
        });
        try {
            return supplier.get();
        } finally {
            RUNNING.compute(thread, (t, deadlines) -> {
                if (deadlines == null) {
                    return null;
                }
                deadlines.remove(deadlineInfo);
                if (deadlines.isEmpty()) {
                    return null;
                } else {
                    return deadlines;
                }
            });
        }
    }

    private void checkDeadline() {
        Set<Thread> changedThreads = new HashSet<>();
        RUNNING.forEach((thread, deadlines) -> {
            for (DeadlineInfo deadline : deadlines) {
                if (deadline.tryCheckDeadlineExceeded()) {
                    // deadlines is a COWArrayList, so using remove instead of iterator.remove
                    deadlines.remove(deadline);
                    changedThreads.add(thread);
                }
            }
        });
        changedThreads.forEach(thread -> { //
            RUNNING.compute(thread, (t, deadlines) -> { //
                if (deadlines == null) {
                    return null;
                }
                if (deadlines.isEmpty()) {
                    return null;
                } else {
                    return deadlines;
                }
            });
        });
    }

    @Override
    public void close() {
        scheduler.tryClose(tuple -> {
            tuple.getSecond().cancel(true);
            shutdownAndAwaitTermination(tuple.getFirst(), 1, MINUTES);
            RUNNING.clear();
        });
    }

    private static class DeadlineInfo {

        private final long startTime;
        private final long deadline;
        private final Thread thread;
        private final Consumer<Thread> deadlineExceeded;

        DeadlineInfo(long deadline, Thread thread, Consumer<Thread> deadlineExceeded) {
            this.startTime = currentTimeMillis();
            this.deadline = deadline;
            this.thread = thread;
            this.deadlineExceeded = checkNotNull(deadlineExceeded);
        }

        /**
         * @return {@code true} if deadline exceeded and execute the consumer.
         */
        boolean tryCheckDeadlineExceeded() {
            if (currentTimeMillis() - startTime >= deadline) {
                try {
                    deadlineExceeded.accept(thread);
                } catch (Throwable e) {
                    logger.error("", e);
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
