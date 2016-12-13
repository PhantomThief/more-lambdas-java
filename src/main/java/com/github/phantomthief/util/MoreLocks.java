package com.github.phantomthief.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * @author w.vela <vela@kuaishou.com>
 * Created on 13/12/2016.
 */
public class MoreLocks {

    private MoreLocks() {
        throw new UnsupportedOperationException();
    }

    public static <X extends Throwable> void runWithLock(Lock lock, ThrowableRunnable<X> runnable)
            throws X {
        supplyWithLock(lock, wrapAsRunnable(runnable));
    }

    public static <T, X extends Throwable> T supplyWithLock(Lock lock,
            ThrowableSupplier<T, X> supplier) throws X {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    public static <X extends Throwable> void runWithTryLock(Lock lock, long time, TimeUnit unit,
            ThrowableRunnable<X> runnable) throws X, InterruptedException {
        runWithTryLock(lock, time, unit, runnable, null);
    }

    public static <X extends Throwable> void runWithTryLock(Lock lock, long time, TimeUnit unit,
            ThrowableRunnable<X> runnable, Runnable withoutAcquiredLock)
            throws X, InterruptedException {
        supplyWithTryLock(lock, time, unit, wrapAsRunnable(runnable), () -> {
            if (withoutAcquiredLock != null) {
                withoutAcquiredLock.run();
            }
            return null;
        });
    }

    public static <T, X extends Throwable> T supplyWithTryLock(Lock lock, long time, TimeUnit unit,
            ThrowableSupplier<T, X> supplier) throws X, InterruptedException {
        return supplyWithTryLock(lock, time, unit, supplier, null);
    }

    public static <T, X extends Throwable> T supplyWithTryLock(Lock lock, long time, TimeUnit unit,
            ThrowableSupplier<T, X> supplier, Supplier<T> withoutAcquiredLock)
            throws X, InterruptedException {
        if (lock.tryLock(time, unit)) {
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        } else {
            if (withoutAcquiredLock != null) {
                return withoutAcquiredLock.get();
            } else {
                return null;
            }
        }
    }

    public static <X extends Throwable> void runWithTryLock(Lock lock,
            ThrowableRunnable<X> runnable) throws X {
        runWithTryLock(lock, runnable, null);
    }

    public static <X extends Throwable> void runWithTryLock(Lock lock,
            ThrowableRunnable<X> runnable, Runnable withoutAcquiredLock) throws X {
        supplyWithTryLock(lock, wrapAsRunnable(runnable), () -> {
            if (withoutAcquiredLock != null) {
                withoutAcquiredLock.run();
            }
            return null;
        });
    }

    public static <T, X extends Throwable> T supplyWithTryLock(Lock lock,
            ThrowableSupplier<T, X> supplier) throws X {
        return supplyWithTryLock(lock, supplier, null);
    }

    public static <T, X extends Throwable> T supplyWithTryLock(Lock lock,
            ThrowableSupplier<T, X> supplier, Supplier<T> withoutAcquiredLock) throws X {
        if (lock.tryLock()) {
            try {
                return supplier.get();
            } finally {
                lock.unlock();
            }
        } else {
            if (withoutAcquiredLock != null) {
                return withoutAcquiredLock.get();
            } else {
                return null;
            }
        }
    }

    private static <X extends Throwable> ThrowableSupplier<Void, X>
            wrapAsRunnable(ThrowableRunnable<X> runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }
}
