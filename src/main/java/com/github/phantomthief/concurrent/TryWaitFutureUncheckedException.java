package com.github.phantomthief.concurrent;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

/**
 * @author w.vela
 * Created on 2018-06-25.
 */
@SuppressWarnings("unchecked")
public class TryWaitFutureUncheckedException extends RuntimeException {

    private final TryWaitResult result;

    TryWaitFutureUncheckedException(TryWaitResult result) {
        this.result = result;
    }

    @Nonnull
    public <V> Map<? extends Future<V>, V> getSuccess() {
        return result.getSuccess();
    }

    @Nonnull
    public Map<? extends Future<?>, Throwable> getFailed() {
        return result.getFailed();
    }

    @Nonnull
    public Map<? extends Future<?>, TimeoutException> getTimeout() {
        return result.getTimeout();
    }

    @Nonnull
    public Map<? extends Future<?>, CancellationException> getCancel() {
        return result.getCancel();
    }

    @Nonnull
    public Map<? extends Future<?>, Boolean> cancelAllTimeout(boolean mayInterruptIfRunning) {
        return result.cancelAllTimeout(mayInterruptIfRunning);
    }
}
