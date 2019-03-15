package com.github.phantomthief.concurrent;

import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

/**
 * @author w.vela
 * Created on 2018-06-25.
 */
class TryWaitResult<K, V> {

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
        Map<K, T2> map = new HashMap<>();
        // not using collect, for value may be null.
        sourceMap.forEach((k, v) -> map.put(transferMap.get(k), v));
        return map;
    }

    @Nonnull
    public Map<K, V> getSuccess() {
        return successMap.get();
    }

    @Nonnull
    public Map<K, Throwable> getFailed() {
        return failedMap.get();
    }

    @Nonnull
    public Map<K, TimeoutException> getTimeout() {
        return timeoutMap.get();
    }

    @Nonnull
    public Map<K, CancellationException> getCancel() {
        return cancelMap.get();
    }

    @Nonnull
    public Map<K, Boolean> cancelAllTimeout(boolean mayInterruptIfRunning) {
        return timeout.entrySet().stream() //
                .collect(toMap(entry -> futureMap.get(entry.getKey()),
                        it -> it.getKey().cancel(mayInterruptIfRunning)));
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

    public String getCombinedExceptionMessage() {
        return Stream.of(getFailed(), getTimeout(), getCancel())
                .map(map -> map.entrySet().stream())
                .flatMap(Function.identity())
                .map(this::exceptionEntryToString)
                .collect(Collectors.joining("\n"));
    }

    private String exceptionEntryToString(Entry<?, ? extends Throwable> entry) {
        return String.format("key:%s, exception:%s, message:%s", entry.getKey(),
                entry.getValue().getClass(), entry.getValue().getMessage());
    }
}
