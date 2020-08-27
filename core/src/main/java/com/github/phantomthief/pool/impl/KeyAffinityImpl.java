package com.github.phantomthief.pool.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.Comparator.comparingInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phantomthief.pool.KeyAffinity;
import com.github.phantomthief.util.ThrowableConsumer;
import com.google.common.annotations.VisibleForTesting;

/**
 * @author w.vela
 * Created on 2018-02-08.
 */
class KeyAffinityImpl<K, V> implements KeyAffinity<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(KeyAffinityImpl.class);
    private static long sleepBeforeClose = SECONDS.toMillis(5);

    private final IntSupplier count;
    private final List<ValueRef> all;
    private final ThrowableConsumer<V, Exception> deposeFunc;
    private final Map<K, KeyRef> mapping = new ConcurrentHashMap<>();
    private final IntPredicate usingRandom;
    private final BooleanSupplier counterChecker;

    private final Supplier<V> supplier;

    KeyAffinityImpl(@Nonnull Supplier<V> supplier, IntSupplier count,
            @Nonnull ThrowableConsumer<V, Exception> deposeFunc, IntPredicate usingRandom,
            BooleanSupplier counterChecker) {
        this.count = count;
        this.usingRandom = usingRandom;
        this.counterChecker = counterChecker;
        this.supplier = supplier;
        this.all = range(0, count.getAsInt())
                .mapToObj(it -> supplier.get())
                .map(ValueRef::new)
                .collect(toCollection(CopyOnWriteArrayList::new));
        this.deposeFunc = checkNotNull(deposeFunc);
    }

    @Nonnull
    @Override
    public V select(K key) {
        int thisCount = count.getAsInt();
        tryCheckCount(thisCount);
        KeyRef keyRef = mapping.compute(key, (k, v) -> {
            if (v == null) {
                if (usingRandom.test(thisCount)) {
                    do {
                        try {
                            v = new KeyRef(all.get(ThreadLocalRandom.current().nextInt(all.size())));
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    } while (v == null);
                } else {
                    v = all.stream()
                            .min(comparingInt(ValueRef::concurrency))
                            .map(KeyRef::new)
                            .orElseThrow(IllegalStateException::new);
                }
            }
            v.incrConcurrency();
            return v;
        });
        return keyRef.ref();
    }

    private void tryCheckCount(int thisCount) {
        if (!counterChecker.getAsBoolean()) {
            return;
        }
        int toAdd = thisCount - all.size();
        if (toAdd == 0) {
            return;
        }
        synchronized (this) {
            toAdd = thisCount - all.size();
            if (toAdd > 0) {
                all.addAll(range(0, toAdd)
                        .mapToObj(it -> supplier.get())
                        .map(ValueRef::new)
                        .collect(toList())
                );
            } else if (toAdd < 0) {
                List<ValueRef> toRemove = new ArrayList<>();
                for (int i = 0; i < -toAdd; i++) {
                    if (all.size() > 0) {
                        ValueRef remove = all.remove(all.size() - 1);
                        toRemove.add(remove);
                    }
                }
                new Thread(() -> {
                    if (sleepBeforeClose > 0) {
                        sleepUninterruptibly(sleepBeforeClose, MILLISECONDS);
                    }
                    for (ValueRef remove : toRemove) {
                        waitAndClose(remove);
                    }
                }, "key affinity removal:" + toRemove.size()).start();
            }
        }
    }

    private void waitAndClose(ValueRef remove) {
        while (remove.concurrency.get() > 0) {
            synchronized (all) {
                try {
                    all.wait(SECONDS.toMillis(1));
                } catch (InterruptedException e) {
                    // just ignore it.
                }
            }
        }
        try {
            deposeFunc.accept(remove.obj);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Override
    public void finishCall(K key) {
        mapping.computeIfPresent(key, (k, v) -> {
            if (v.decrConcurrency()) {
                return null;
            } else {
                return v;
            }
        });
    }

    @Override
    public boolean inited() {
        return true;
    }

    @Override
    public void close() throws Exception {
        synchronized (all) {
            while (all.stream().anyMatch(it -> it.concurrency.get() > 0)) {
                all.wait(SECONDS.toMillis(1));
            }
        }
        for (ValueRef ref : all) {
            deposeFunc.accept(ref.obj);
        }
    }

    @Override
    public Iterator<V> iterator() {
        return transform(all.iterator(), v -> v.obj);
    }

    private class KeyRef {

        private final ValueRef valueRef;
        private final AtomicInteger concurrency = new AtomicInteger();

        KeyRef(ValueRef valueRef) {
            this.valueRef = valueRef;
        }

        void incrConcurrency() {
            concurrency.incrementAndGet();
            valueRef.concurrency.incrementAndGet();
        }

        /**
         * @return {@code true} if no ref by key
         */
        boolean decrConcurrency() {
            int r = concurrency.decrementAndGet();
            int refConcurrency = valueRef.concurrency.decrementAndGet();
            if (refConcurrency <= 0) {
                synchronized (all) {
                    all.notifyAll();
                }
            }
            return r <= 0;
        }

        V ref() {
            return valueRef.obj;
        }
    }

    // for mock and test.
    @VisibleForTesting
    static void setSleepBeforeClose(long sleepBeforeClose) {
        KeyAffinityImpl.sleepBeforeClose = sleepBeforeClose;
    }

    private class ValueRef {

        private final V obj;
        private final AtomicInteger concurrency = new AtomicInteger();

        ValueRef(V obj) {
            this.obj = obj;
        }

        int concurrency() {
            return concurrency.get();
        }
    }
}
