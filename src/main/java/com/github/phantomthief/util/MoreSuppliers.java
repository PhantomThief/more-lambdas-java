/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author w.vela
 */
public final class MoreSuppliers {

    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate) {
        return lazy(delegate, true);
    }

    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate, boolean resetAfterClose) {
        if (delegate instanceof CloseableSupplier) {
            return (CloseableSupplier<T>) delegate;
        } else {
            return new CloseableSupplier<>(checkNotNull(delegate), resetAfterClose);
        }
    }

    public static <T> Supplier<T> asyncLazy(Supplier<T> delegate, Supplier<T> pendingSupplier,
            String threadName) {
        return new AsyncSupplier<>(delegate, pendingSupplier, threadName);
    }

    public static <T> Supplier<T> asyncLazy(Supplier<T> delegate, String threadName) {
        return asyncLazy(delegate, () -> null, threadName);
    }

    public static <T> Supplier<T> asyncLazy(Supplier<T> delegate) {
        return asyncLazy(delegate, null);
    }

    public static class CloseableSupplier<T> implements Supplier<T>, Serializable {

        private static final long serialVersionUID = 0L;
        private final Supplier<T> delegate;
        private final boolean resetAfterClose;
        private volatile transient boolean initialized;
        private transient T value;

        private CloseableSupplier(Supplier<T> delegate, boolean resetAfterClose) {
            this.delegate = delegate;
            this.resetAfterClose = resetAfterClose;
        }

        public T get() {
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

        public boolean isInitialized() {
            return initialized;
        }

        public <X extends Throwable> void ifPresent(ThrowableConsumer<T, X> consumer) throws X {
            synchronized (this) {
                if (initialized && this.value != null) {
                    consumer.accept(this.value);
                }
            }
        }

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

        public void tryClose() {
            tryClose(i -> {});
        }

        public <X extends Throwable> void tryClose(ThrowableConsumer<T, X> close) throws X {
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
                return "MoreSuppliers.lazy(" + get() + ")";
            } else {
                return "MoreSuppliers.lazy(" + this.delegate + ")";
            }
        }
    }

    public static final class AsyncSupplier<T> implements Supplier<T> {

        private final String initThreadName;
        private final Supplier<T> innerSupplier;
        private final Supplier<T> pendingSupplier;

        private volatile T value;

        private volatile boolean inited;
        private volatile boolean initing;

        AsyncSupplier(Supplier<T> innerSupplier, Supplier<T> pendingSupplier,
                String initThreadName) {
            this.initThreadName = initThreadName;
            this.innerSupplier = checkNotNull(innerSupplier);
            this.pendingSupplier = checkNotNull(pendingSupplier);
        }

        @Override
        public T get() {
            if (inited) {
                return value;
            }
            if (initing) {
                return pendingSupplier.get();
            }
            synchronized (this) {
                if (inited) {
                    return value;
                }
                if (initing) {
                    return pendingSupplier.get();
                }
                initing = true;
                Runnable initWithTry = () -> {
                    try {
                        value = innerSupplier.get();
                        inited = true;
                        initing = false;
                    } catch (Throwable e) {
                        initing = false;
                        throw e;
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
                return pendingSupplier.get();
            }
        }
    }
}
