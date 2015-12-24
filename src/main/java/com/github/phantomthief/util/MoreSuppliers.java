/**
 * 
 */
package com.github.phantomthief.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * @author w.vela
 */
public class MoreSuppliers {

    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate) {
        return new CloseableSupplier<T>(checkNotNull(delegate));
    }

    public static class CloseableSupplier<T> implements Supplier<T>, Serializable {

        private final Supplier<T> delegate;
        private volatile transient boolean initialized;
        private transient T value;
        private static final long serialVersionUID = 0L;

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

        public String toString() {
            return "MoreSuppliers.lazy(" + this.delegate + ")";
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
    }
}
