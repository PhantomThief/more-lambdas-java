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
public class MoreSuppliers {

    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate) {
        if (delegate instanceof CloseableSupplier) {
            return (CloseableSupplier<T>) delegate;
        } else {
            return new CloseableSupplier<>(checkNotNull(delegate));
        }
    }

    public static class CloseableSupplier<T> implements Supplier<T>, Serializable {

        private static final long serialVersionUID = 0L;
        private final Supplier<T> delegate;
        private volatile transient boolean initialized;
        private transient T value;

        private CloseableSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
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

        public <X extends Throwable> void tryClose(ThrowableConsumer<T, X> close) throws X {
            synchronized (this) {
                if (initialized && this.value != null) {
                    close.accept(value);
                    this.value = null;
                    initialized = false;
                }
            }
        }

        public String toString() {
            return "MoreSuppliers.lazy(" + this.delegate + ")";
        }
    }
}
