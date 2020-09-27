package com.github.phantomthief.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * MoreLocks增强工具集合
 * <p>锁快捷工具，锁控制的函数运行</p>
 *
 * @author w.vela
 */
public final class MoreLocks {

    /**
     * 工具类，禁止实例化成对象
     */
    private MoreLocks() {
        throw new UnsupportedOperationException();
    }

    /**
     * 使用指定的锁运行函数，先获取锁，执行完成后释放
     *
     * @param lock 锁
     * @param runnable 执行函数
     * @param <X> 执行的异常类型泛型
     * @throws X 执行异常
     */
    public static <X extends Throwable> void runWithLock(Lock lock, ThrowableRunnable<X> runnable)
            throws X {
        supplyWithLock(lock, wrapAsRunnable(runnable));
    }

    /**
     * 使用指定的锁运行函数，先获取锁，执行完成后释放
     *
     * @param lock 锁
     * @param supplier 执行函数
     * @param <T> 返回值类型
     * @param <X> 执行的异常类型泛型
     * @return 函数执行的返回值结果
     * @throws X 执行异常
     */
    public static <T, X extends Throwable> T supplyWithLock(Lock lock,
            ThrowableSupplier<T, X> supplier) throws X {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 使用指定的锁运行函数，先尝试获取锁，等待指定的时间，执行完成后释放
     *
     * @param lock 锁对象
     * @param time 等待时长
     * @param unit 等待时长的时间单位
     * @param runnable 执行函数
     * @param <X> 执行的异常类型泛型
     * @throws X 执行异常
     * @throws InterruptedException 锁获取打断的异常
     */
    public static <X extends Throwable> void runWithTryLock(Lock lock, long time, TimeUnit unit,
            ThrowableRunnable<X> runnable) throws X, InterruptedException {
        runWithTryLock(lock, time, unit, runnable, null);
    }

    /**
     * 使用指定的锁运行函数，先尝试获取锁，等待指定的时间，执行完成后释放，可指定未获取到锁时的处理函数
     *
     * @param lock 锁对象
     * @param time 等待时长
     * @param unit 等待时长的时间单位
     * @param runnable 执行函数
     * @param withoutAcquiredLock 未获取到锁时执行的函数
     * @param <X> 执行的异常类型泛型
     * @throws X 执行异常
     * @throws InterruptedException 锁获取打断的异常
     */
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

    /**
     * 使用指定的锁运行函数，先尝试获取锁，等待指定的时间，执行完成后释放
     *
     * @param lock 锁对象
     * @param time 等待时长
     * @param unit 等待时长的时间单位
     * @param supplier 执行函数
     * @param <T> 执行函数的返回值结果类型泛型
     * @param <X> 执行的异常类型泛型
     * @return 执行函数的返回值
     * @throws X 执行异常
     * @throws InterruptedException 锁获取打断的异常
     */
    public static <T, X extends Throwable> T supplyWithTryLock(Lock lock, long time, TimeUnit unit,
            ThrowableSupplier<T, X> supplier) throws X, InterruptedException {
        return supplyWithTryLock(lock, time, unit, supplier, null);
    }

    /**
     * 使用指定的锁运行函数，先尝试获取锁，等待指定的时间，执行完成后释放，可指定未获取到锁时的处理函数
     *
     * @param lock 锁对象
     * @param time 等待时长
     * @param unit 等待时长的时间单位
     * @param supplier 执行函数
     * @param withoutAcquiredLock 未获取到锁时执行的函数
     * @param <T> 执行函数的返回值结果类型泛型
     * @param <X> 执行的异常类型泛型
     * @return 执行函数的返回值
     * @throws X 执行异常
     * @throws InterruptedException 锁获取打断的异常
     */
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

    /**
     * 使用指定的锁运行函数，先尝试获取锁，不等待，执行完成后释放
     *
     * @param lock 锁对象
     * @param runnable 执行函数
     * @param <X> 执行的异常类型泛型
     * @throws X 执行异常
     */
    public static <X extends Throwable> void runWithTryLock(Lock lock,
            ThrowableRunnable<X> runnable) throws X {
        runWithTryLock(lock, runnable, null);
    }

    /**
     * 使用指定的锁运行函数，先尝试获取锁，不等待，执行完成后释放，可指定未获取到锁时的处理函数
     *
     * @param lock 锁对象
     * @param runnable 执行函数
     * @param withoutAcquiredLock 未获取到锁时执行的函数
     * @param <X> 执行的异常类型泛型
     * @throws X 执行异常
     */
    public static <X extends Throwable> void runWithTryLock(Lock lock,
            ThrowableRunnable<X> runnable, Runnable withoutAcquiredLock) throws X {
        supplyWithTryLock(lock, wrapAsRunnable(runnable), () -> {
            if (withoutAcquiredLock != null) {
                withoutAcquiredLock.run();
            }
            return null;
        });
    }

    /**
     * 使用指定的锁运行函数，先尝试获取锁，不等待，执行完成后释放
     *
     * @param lock 锁对象
     * @param supplier 执行函数
     * @param <T> 执行函数的返回值结果类型泛型
     * @param <X> 执行的异常类型泛型
     * @return 执行函数的返回值
     * @throws X 执行异常
     */
    public static <T, X extends Throwable> T supplyWithTryLock(Lock lock,
            ThrowableSupplier<T, X> supplier) throws X {
        return supplyWithTryLock(lock, supplier, null);
    }

    /**
     * 使用指定的锁运行函数，先尝试获取锁，不等待，执行完成后释放，可指定未获取到锁时的处理函数
     *
     * @param lock 锁对象
     * @param supplier 执行函数
     * @param withoutAcquiredLock 未获取到锁时执行的函数
     * @param <T> 执行函数的返回值结果类型泛型
     * @param <X> 执行的异常类型泛型
     * @return 执行函数的返回值
     * @throws X 执行异常
     */
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

    /**
     * 将{@link ThrowableRunnable}包装为一个{@link ThrowableSupplier}
     *
     * @param runnable 输入的{@link ThrowableRunnable}
     * @param <X> 执行的异常类型泛型
     * @return 输出的{@link ThrowableSupplier}，它返回值为null
     */
    private static <X extends Throwable> ThrowableSupplier<Void, X>
            wrapAsRunnable(ThrowableRunnable<X> runnable) {
        return () -> {
            runnable.run();
            return null;
        };
    }
}
