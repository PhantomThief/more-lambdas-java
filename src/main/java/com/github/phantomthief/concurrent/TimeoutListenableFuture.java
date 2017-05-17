package com.github.phantomthief.concurrent;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

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
    private final List<ThrowableRunnable<Exception>> timeoutListeners = new ArrayList<>();

    public TimeoutListenableFuture(ListenableFuture<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected ListenableFuture<V> delegate() {
        return delegate;
    }

    public TimeoutListenableFuture<V> addTimeoutListener(ThrowableRunnable<Exception> consumer) {
        timeoutListeners.add(consumer);
        return this;
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return delegate().get(timeout, unit);
        } catch (TimeoutException e) {
            for (ThrowableRunnable<Exception> timeoutListener : timeoutListeners) {
                try {
                    timeoutListener.run();
                } catch (Exception e1) {
                    logger.error("", e1);
                }
            }
            throw e;
        }
    }
}
