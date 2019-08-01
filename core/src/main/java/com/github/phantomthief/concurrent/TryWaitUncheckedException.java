package com.github.phantomthief.concurrent;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

/**
 * @author w.vela
 * Created on 2018-06-25.
 */
@SuppressWarnings("unchecked")
public class TryWaitUncheckedException extends RuntimeException {

    private final TryWaitResult result;

    TryWaitUncheckedException(TryWaitResult result) {
        this.result = result;
    }

    @Nonnull
    public <K, V> Map<K, V> getSuccess() {
        return result.getSuccess();
    }

    @Nonnull
    public <K> Map<K, Throwable> getFailed() {
        return result.getFailed();
    }

    @Nonnull
    public <K> Map<K, TimeoutException> getTimeout() {
        return result.getTimeout();
    }

    @Nonnull
    public <K> Map<K, CancellationException> getCancel() {
        return result.getCancel();
    }

    @Nonnull
    public <K> Map<K, Boolean> cancelAllTimeout(boolean mayInterruptIfRunning) {
        return result.cancelAllTimeout(mayInterruptIfRunning);
    }

    @Override
    public String getMessage() {
        return result.getCombinedExceptionMessage();
    }

}
