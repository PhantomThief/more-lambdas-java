package com.github.phantomthief.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.github.phantomthief.util.ThrowableConsumer;
import com.github.phantomthief.util.ThrowableRunnable;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author w.vela
 * Created on 16/5/31.
 */
public class TimeoutListenableFuture<V> extends ForwardingListenableFuture<V> {

    private static final Logger logger = getLogger(TimeoutListenableFuture.class);

    private final ListenableFuture<V> delegate;
    private final List<ThrowableConsumer<TimeoutException, Exception>> timeoutListeners = new ArrayList<>();

    /**
     * better use {@link #timeoutListenableFuture(ListenableFuture)}
     */
    public TimeoutListenableFuture(ListenableFuture<V> delegate) {
        this.delegate = delegate;
    }

    public static <V> TimeoutListenableFuture<V>
            timeoutListenableFuture(ListenableFuture<V> delegate) {
        if (delegate instanceof TimeoutListenableFuture) {
            return (TimeoutListenableFuture<V>) delegate;
        } else {
            return new TimeoutListenableFuture<>(delegate);
        }
    }

    @Override
    protected ListenableFuture<V> delegate() {
        return delegate;
    }

    public TimeoutListenableFuture<V>
            addTimeoutListener(@Nonnull ThrowableRunnable<Exception> listener) {
        checkNotNull(listener);
        return addTimeoutListener(e -> listener.run());
    }

    public TimeoutListenableFuture<V>
            addTimeoutListener(@Nonnull ThrowableConsumer<TimeoutException, Exception> listener) {
        timeoutListeners.add(checkNotNull(listener));
        return this;
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return delegate().get(timeout, unit);
        } catch (TimeoutException e) {
            for (ThrowableConsumer<TimeoutException, Exception> listener : timeoutListeners) {
                try {
                    listener.accept(e);
                } catch (Exception e1) {
                    logger.error("", e1);
                }
            }
            throw e;
        }
    }

    public List<ThrowableConsumer<TimeoutException, Exception>> getTimeoutListeners() {
        return Collections.unmodifiableList(timeoutListeners);
    }
}
