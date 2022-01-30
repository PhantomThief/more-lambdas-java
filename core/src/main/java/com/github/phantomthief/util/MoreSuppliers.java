package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static java.lang.System.nanoTime;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * MoreSuppliers增强工具
 * <p>{@link Supplier}的增强函数，使{@link Supplier}执行的执行结果被缓存，真正的调用只执行一次。</p>
 *
 * @author w.vela
 */
public final class MoreSuppliers {

    /**
     * 提供器懒加载工具
     * <p>增强{@link Supplier}，首次获取值时通过{@link Supplier}加载值，并缓存这个值，在后续获取时直接返回这个缓存的值。</p>
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     */
    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate) {
        return lazy(delegate, true);
    }

    /**
     * 提供器懒加载工具，支持指定是否释放缓存对象
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param resetAfterClose 在关闭提供器返回的资源后，是否释放缓存的对象
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     * @see MoreSuppliers#lazy(java.util.function.Supplier)
     */
    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate, boolean resetAfterClose) {
        if (delegate instanceof CloseableSupplier) {
            return (CloseableSupplier<T>) delegate;
        } else {
            return new CloseableSupplier<>(checkNotNull(delegate), resetAfterClose);
        }
    }

    /**
     * 提供器懒加载工具，支持异常类型声明
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param <T> 提供值的泛型类型
     * @param <X> 加载异常泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     */
    public static <T, X extends Throwable> CloseableThrowableSupplier<T, X> lazyEx(ThrowableSupplier<T, X> delegate) {
        return lazyEx(delegate, true);
    }

    /**
     * 提供器懒加载工具，支持异常类型声明和指定是否释放缓存对象
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param resetAfterClose 在关闭提供器返回的资源后，是否释放缓存的对象
     * @param <T> 提供值的泛型类型
     * @param <X> 加载异常泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     */
    public static <T, X extends Throwable> CloseableThrowableSupplier<T, X> lazyEx(ThrowableSupplier<T, X> delegate,
            boolean resetAfterClose) {
        if (delegate instanceof CloseableThrowableSupplier) {
            return (CloseableThrowableSupplier<T, X>) delegate;
        } else {
            return new CloseableThrowableSupplier<>(checkNotNull(delegate), resetAfterClose);
        }
    }

    /**
     * 已废弃：使用异步线程加载的提供器懒加载工具
     * <p>请使用{@link #asyncLazyEx(Supplier, Supplier, String)}替代本方法</p>
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param pendingSupplier 当超过指定的时间没有获取初始值成功时，使用此提供器提供的值作为托底。此值不会缓存，不影响下次获取时再次尝试初始化
     * @param threadName 用于异步初始化的线程名称
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     */
    @Deprecated
    public static <T> Supplier<T> asyncLazy(Supplier<T> delegate, Supplier<T> pendingSupplier,
            String threadName) {
        return new AsyncSupplier<>(delegate, pendingSupplier, threadName);
    }

    /**
     * 已废弃：使用异步线程加载的提供器懒加载工具
     * <p>请使用{@link #asyncLazyEx(Supplier, String)}替代本方法</p>
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param threadName 用于异步初始化的线程名称
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     */
    @Deprecated
    public static <T> Supplier<T> asyncLazy(Supplier<T> delegate, String threadName) {
        return asyncLazy(delegate, () -> null, threadName);
    }

    /**
     * 已废弃：使用异步线程加载的提供器懒加载工具
     * <p>请使用{@link #asyncLazyEx(Supplier)}替代本方法</p>
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，与原提供器兼容
     */
    @Deprecated
    public static <T> Supplier<T> asyncLazy(Supplier<T> delegate) {
        return asyncLazy(delegate, null);
    }

    /**
     * 使用异步线程加载的提供器懒加载工具
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param pendingSupplier 当超过指定的时间没有获取初始值成功时，使用此提供器提供的值作为托底。此值不会缓存，不影响下次获取时再次尝试初始化
     * @param threadName 用于异步初始化的线程名称
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，可通过{@link AsyncSupplier#get(java.time.Duration)}方法实现获取超时
     */
    public static <T> AsyncSupplier<T> asyncLazyEx(Supplier<T> delegate, Supplier<T> pendingSupplier,
            String threadName) {
        return new AsyncSupplier<>(delegate, pendingSupplier, threadName);
    }

    /**
     * 使用异步线程加载的提供器懒加载工具
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param threadName 用于异步初始化的线程名称
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，可通过{@link AsyncSupplier#get(java.time.Duration)}方法实现获取超时
     */
    public static <T> AsyncSupplier<T> asyncLazyEx(Supplier<T> delegate, String threadName) {
        return asyncLazyEx(delegate, () -> null, threadName);
    }

    /**
     * 使用异步线程加载的提供器懒加载工具
     *
     * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载，不能为空
     * @param <T> 提供值的泛型类型
     * @return 返回一个懒加载的提供器，可通过{@link AsyncSupplier#get(java.time.Duration)}方法实现获取超时
     */
    public static <T> AsyncSupplier<T> asyncLazyEx(Supplier<T> delegate) {
        return asyncLazyEx(delegate, null);
    }

    /**
     * 可关闭的Supplier实现
     * <p>支持通过{@link CloseableSupplier#tryClose(com.github.phantomthief.util.ThrowableConsumer)}关闭提供的资源</p>
     *
     * @param <T> 提供值的泛型
     */
    public static class CloseableSupplier<T> implements Supplier<T>, Serializable {

        private static final long serialVersionUID = 0L;

        /**
         * 原始{@link Supplier}对象，用于提供值的首次加载
         */
        private final Supplier<T> delegate;

        /**
         * 是否在执行{@link #tryClose()}操作时释放缓存的对象
         */
        private final boolean resetAfterClose;

        /**
         * 当前是否已经获取过提供器的值
         */
        private transient volatile boolean initialized;
        // TODO initialized 和 closing 可以复用同一个 int，按位检查状态，提高性能
        private transient volatile boolean closing;

        /**
         * 当前缓存的提供器的值
         */
        private transient T value;

        /**
         * 构造方法：创建一个CloseableSupplier
         *
         * @param delegate 原始{@link Supplier}对象，用于提供值的首次加载
         * @param resetAfterClose 是否在执行{@link #tryClose()}操作时释放缓存的对象
         */
        private CloseableSupplier(Supplier<T> delegate, boolean resetAfterClose) {
            this.delegate = delegate;
            this.resetAfterClose = resetAfterClose;
        }

        /**
         * 获取当前值
         * <p>如果当前值没有获取过，将通过{@link #delegate}提供器加载一次获取值，如果获取过，则直接返回上次获取的值</p>
         *
         * @return 提供器的值
         */
        public T get() {
            if (!(this.initialized) || closing) {
                synchronized (this) {
                    if (!(this.initialized)) {
                        T t = this.delegate.get();
                        this.value = t;
                        this.initialized = true;
                        return t;
                    }
                }
            }
            return this.value;
        }

        /**
         * 返回当前提供器是否获取过初始值
         *
         * @return 是否获取过初始值
         */
        public boolean isInitialized() {
            return initialized;
        }

        /**
         * 使用当前缓存的值
         *
         * @param consumer 当前值的消费函数，不能为空
         * @param <X> 消费异常泛型
         * @throws X 消费抛出的异常
         */
        public <X extends Throwable> void ifPresent(ThrowableConsumer<T, X> consumer) throws X {
            synchronized (this) {
                if (initialized && this.value != null) {
                    consumer.accept(this.value);
                }
            }
        }

        /**
         * 将当前初始化的值转换类型并返回
         *
         * @param mapper 类型映射器，不能为空
         * @param <U> 目标类型泛型
         * @return 转换值的容器
         */
        public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
            checkNotNull(mapper);
            synchronized (this) {
                if (initialized && this.value != null) {
                    return ofNullable(mapper.apply(value));
                } else {
                    return empty();
                }
            }
        }

        /**
         * 尝试释放当前缓存的值，是否真正执行释放取决于{@link #resetAfterClose}属性
         */
        public void tryClose() {
            tryClose(i -> { });
        }

        /**
         * 尝试释放当前缓存的值，是否真正执行释放取决于{@link #resetAfterClose}属性，在释放前执行一个函数以帮助销毁资源等操作
         *
         * @param close 关闭函数，用于销毁资源等操作
         * @param <X> 关闭时产生的异常的泛型
         * @throws X 关闭时产生的异常
         */
        public <X extends Throwable> void tryClose(ThrowableConsumer<T, X> close) throws X {
            synchronized (this) {
                if (initialized) {
                    if (resetAfterClose) {
                        closing = true;
                    }
                    try {
                        close.accept(value);
                        if (resetAfterClose) {
                            this.value = null;
                            initialized = false;
                        }
                    } finally {
                        closing = false;
                    }
                }
            }
        }

        public String toString() {
            if (initialized) {
                return "MoreSuppliers.lazy(" + get() + ")";
            } else {
                return "MoreSuppliers.lazy(" + this.delegate + ")";
            }
        }
    }

    /**
     * 支持操作异常声明的可关闭的Supplier实现
     *
     * @param <T> 提供值泛型
     * @param <X> 操作异常泛型
     */
    public static class CloseableThrowableSupplier<T, X extends Throwable>
            implements ThrowableSupplier<T, X>, Serializable {

        private static final long serialVersionUID = 0L;
        /**
         * 原始{@link ThrowableSupplier}对象，用于提供值的首次加载的值
         */
        private final ThrowableSupplier<T, X> delegate;

        /**
         * 是否在执行{@link #tryClose()}操作时释放缓存的对象
         */
        private final boolean resetAfterClose;

        /**
         * 当前是否已经获取过提供器的值
         */
        private transient volatile boolean initialized;

        /**
         * 当前缓存的提供器的值
         */
        private transient T value;

        /**
         * 构造方法：创建一个CloseableThrowableSupplier
         *
         * @param delegate 原始{@link ThrowableSupplier}对象，用于提供值的首次加载
         * @param resetAfterClose 是否在执行{@link #tryClose()}操作时释放缓存的对象
         */
        private CloseableThrowableSupplier(ThrowableSupplier<T, X> delegate, boolean resetAfterClose) {
            this.delegate = delegate;
            this.resetAfterClose = resetAfterClose;
        }

        /**
         * 获取当前值
         * <p>如果当前值没有获取过，将通过{@link #delegate}提供器加载一次获取值，如果获取过，则直接返回上次获取的值</p>
         *
         * @return 提供器的值
         */
        public T get() throws X {
            if (!(this.initialized)) {
                synchronized (this) {
                    if (!(this.initialized)) {
                        T t = this.delegate.get();
                        this.value = t;
                        this.initialized = true;
                        return t;
                    }
                }
            }
            return this.value;
        }

        /**
         * 返回当前提供器是否获取过初始值
         *
         * @return 是否获取过初始值
         */
        public boolean isInitialized() {
            return initialized;
        }

        /**
         * 使用当前缓存的值
         *
         * @param consumer 当前值的消费函数，不能为空
         * @param <X2> 消费异常泛型
         * @throws X2 消费抛出的异常
         */
        public <X2 extends Throwable> void ifPresent(ThrowableConsumer<T, X2> consumer) throws X2 {
            synchronized (this) {
                if (initialized && this.value != null) {
                    consumer.accept(this.value);
                }
            }
        }

        /**
         * 将当前初始化的值转换类型并返回
         *
         * @param mapper 类型映射器，不能为空
         * @param <U> 目标类型泛型
         * @return 转换值的容器
         */
        public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
            checkNotNull(mapper);
            synchronized (this) {
                if (initialized && this.value != null) {
                    return ofNullable(mapper.apply(value));
                } else {
                    return empty();
                }
            }
        }

        /**
         * 尝试释放当前缓存的值，是否真正执行释放取决于{@link #resetAfterClose}属性
         */
        public void tryClose() {
            tryClose(i -> { });
        }

        /**
         * 尝试释放当前缓存的值，是否真正执行释放取决于{@link #resetAfterClose}属性，在释放前执行一个函数以帮助销毁资源等操作
         *
         * @param close 关闭函数，用于销毁资源等操作
         * @param <X2> 关闭时产生的异常的泛型
         * @throws X2 关闭时产生的异常
         */
        public <X2 extends Throwable> void tryClose(ThrowableConsumer<T, X2> close) throws X2 {
            synchronized (this) {
                if (initialized) {
                    close.accept(value);
                    if (resetAfterClose) {
                        this.value = null;
                        initialized = false;
                    }
                }
            }
        }

        public String toString() {
            if (initialized) {
                try {
                    return "MoreSuppliers.lazy(" + get() + ")";
                } catch (Throwable x) {
                    throw new RuntimeException(x);
                }
            } else {
                return "MoreSuppliers.lazy(" + this.delegate + ")";
            }
        }
    }

    /**
     * 异步加载的提供器实现，通过异步线程来完成初始化操作，支持超时
     *
     * @param <T> 提供值的泛型
     */
    public static final class AsyncSupplier<T> implements Supplier<T> {

        /**
         * 执行初始化操作的线程名称
         */
        private final String initThreadName;

        /**
         * 原始{@link Supplier}对象，用于提供值的首次加载
         */
        private final Supplier<T> innerSupplier;

        /**
         * 托底的{@link Supplier}对象，当异步初始化值超时时，通过此提供器获取返回值，此提供器的返回值不会被缓存
         */
        private final Supplier<T> pendingSupplier;

        /**
         * 当前缓存的提供器的值
         */
        private volatile T value;

        /**
         * 当前是否已经初始化
         */
        private volatile boolean inited;

        /**
         * 当前是否正在进行异步初始化
         */
        private volatile boolean initing;

        /**
         * 首次执行初始化的启动时间
         */
        private volatile long firstInitNano;

        /**
         * 用于初始化线程工作状态同步的信号量
         */
        private volatile CountDownLatch latch;

        /**
         * 构造方法：创建AsyncSupplier对象
         *
         * @param innerSupplier 原始{@link ThrowableSupplier}对象，用于提供值的首次加载
         * @param pendingSupplier 托底的{@link Supplier}对象，当异步初始化值超时时，通过此提供器获取返回值，此提供器的返回值不会被缓存
         * @param initThreadName 执行初始化操作的线程名称
         */
        AsyncSupplier(Supplier<T> innerSupplier, Supplier<T> pendingSupplier,
                String initThreadName) {
            this.initThreadName = initThreadName;
            this.innerSupplier = checkNotNull(innerSupplier);
            this.pendingSupplier = checkNotNull(pendingSupplier);
        }

        /**
         * 获取当前值，支持设置超时时间
         * <p>如果当前值没有获取过，将通过{@link #innerSupplier}提供器加载一次获取值，如果获取过，则直接返回上次获取的值；
         * 如果获取操作超过指定等待的获取时间，则直接通过{@link #pendingSupplier}获取托底值</p>
         *
         * @param timeoutFromFirstIniting 等待获取的超时时间，获取时间从首次开始执行异步获取算起，
         * 传入null将不等待，如果初始化成功返回初始化的值，否则返回托底值
         */
        public T get(@Nullable Duration timeoutFromFirstIniting) {
            if (inited) {
                return value;
            }
            if (initing) {
                tryWait(timeoutFromFirstIniting);
                if (inited) {
                    return value;
                }
                return pendingSupplier.get();
            }
            synchronized (this) {
                if (inited) {
                    return value;
                }
                if (initing) {
                    tryWait(timeoutFromFirstIniting);
                    if (inited) {
                        return value;
                    }
                    return pendingSupplier.get();
                }
                // start init
                firstInitNano = nanoTime();
                latch = new CountDownLatch(1);
                initing = true;
                Runnable initWithTry = () -> {
                    try {
                        value = innerSupplier.get();
                        inited = true;
                        initing = false;
                    } catch (Throwable e) {
                        initing = false;
                        throw e;
                    } finally {
                        latch.countDown();
                    }
                };
                if (initThreadName == null) {
                    new Thread(initWithTry).start();
                } else {
                    new Thread(initWithTry, initThreadName).start();
                }
            }
            if (inited) {
                return value;
            } else {
                tryWait(timeoutFromFirstIniting);
                if (inited) {
                    return value;
                }
                return pendingSupplier.get();
            }
        }

        /**
         * 直接获取当前加载的值，不进行等待
         *
         * @see AsyncSupplier#get(java.time.Duration)
         */
        @Override
        public T get() {
            return get(null);
        }

        /**
         * 等待指定的时间完成异步加载
         *
         * @param maxWaitFromFirstCall 等待时长
         */
        private void tryWait(@Nullable Duration maxWaitFromFirstCall) {
            if (maxWaitFromFirstCall == null) {
                return;
            }
            long passedDuration = nanoTime() - firstInitNano;
            long maxWaitDuration = maxWaitFromFirstCall.toNanos();
            long needWaitDuration = maxWaitDuration - passedDuration;
            if (needWaitDuration > 0) {
                awaitUninterruptibly(latch, needWaitDuration, NANOSECONDS);
            }
        }
    }
}
