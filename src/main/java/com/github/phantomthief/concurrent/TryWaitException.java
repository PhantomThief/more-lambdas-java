package com.github.phantomthief.concurrent;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * @author w.vela
 * Created on 2018-06-25.
 */
@SuppressWarnings("unchecked")
public class TryWaitException extends Exception {

    private final TryWaitResult result;

    TryWaitException(TryWaitResult result) {
        this.result = result;
    }

    public <T> Map<Future<T>, Throwable> getFailed() {
        return result.getFailed();
    }

    public <T> Map<Future<T>, TimeoutException> getTimeout() {
        return result.getTimeout();
    }

    public <T> Map<Future<T>, CancellationException> getCancel() {
        return result.getCancel();
    }
}
