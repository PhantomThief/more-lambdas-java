package com.github.phantomthief.concurrent;

import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * @author w.vela
 * Created on 2018-06-25.
 */
public class TryWaitResult<K, V> {

    private final Map<Future<? extends V>, V> success;
    private final Map<Future<? extends V>, Throwable> failed;
    private final Map<Future<? extends V>, TimeoutException> timeout;
    private final Map<Future<? extends V>, CancellationException> cancel;
    private final Map<Future<? extends V>, K> futureMap;

    private final Supplier<Map<K, V>> successMap;
    private final Supplier<Map<K, Throwable>> failedMap;
    private final Supplier<Map<K, TimeoutException>> timeoutMap;
    private final Supplier<Map<K, CancellationException>> cancelMap;

    TryWaitResult(Map<Future<? extends V>, V> success, Map<Future<? extends V>, Throwable> failed,
            Map<Future<? extends V>, TimeoutException> timeout,
            Map<Future<? extends V>, CancellationException> cancel,
            Map<Future<? extends V>, K> futureMap) {
        this.success = success;
        this.failed = failed;
        this.timeout = timeout;
        this.cancel = cancel;
        this.futureMap = futureMap;
        successMap = lazy(() -> transfer(this.success, this.futureMap));
        failedMap = lazy(() -> transfer(this.failed, this.futureMap));
        timeoutMap = lazy(() -> transfer(this.timeout, this.futureMap));
        cancelMap = lazy(() -> transfer(this.cancel, this.futureMap));
    }

    private <T2> Map<K, T2> transfer(Map<Future<? extends V>, T2> sourceMap,
            Map<Future<? extends V>, K> transferMap) {
        return sourceMap.entrySet().stream() //
                .collect(toMap(entry -> transferMap.get(entry.getKey()), Map.Entry::getValue));
    }

    public Map<K, V> getSuccess() {
        return successMap.get();
    }

    public Map<K, Throwable> getFailed() {
        return failedMap.get();
    }

    public Map<K, TimeoutException> getTimeout() {
        return timeoutMap.get();
    }

    public Map<K, CancellationException> getCancel() {
        return cancelMap.get();
    }

    public Map<K, Boolean> cancelAllTimeout(boolean mayInterruptIfRunning) {
        return timeout.entrySet().stream() //
                .collect(toMap(entry -> futureMap.get(entry.getKey()),
                        it -> it.getKey().cancel(mayInterruptIfRunning)));
    }

    public void orThrow() throws TryWaitException {
        if (!failed.isEmpty() || !timeout.isEmpty() || !cancel.isEmpty()) {
            throw new TryWaitException(this);
        }
    }

    public void orThrowUnchecked() throws TryWaitUncheckedException {
        if (!failed.isEmpty() || !timeout.isEmpty() || !cancel.isEmpty()) {
            throw new TryWaitUncheckedException(this);
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this) //
                .add("success", success.size()) //
                .add("failed", failed.size()) //
                .add("timeout", timeout.size()) //
                .add("cancel", cancel.size()) //
                .toString();
    }
}
