package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.util.MoreSuppliers.lazy;

import java.util.Iterator;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.github.phantomthief.pool.KeyAffinity;
import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;

/**
 * @author w.vela
 * Created on 2018-02-08.
 */
class LazyKeyAffinity<K, V> implements KeyAffinity<K, V> {

    private final CloseableSupplier<KeyAffinity<K, V>> factory;

    LazyKeyAffinity(Supplier<KeyAffinity<K, V>> factory) {
        this.factory = lazy(factory, false);
    }

    @Nonnull
    @Override
    public V select(K key) {
        return factory.get().select(key);
    }

    @Override
    public void finishCall(K key) {
        factory.get().finishCall(key);
    }

    @Override
    public boolean inited() {
        return factory.isInitialized();
    }

    @Override
    public void close() throws Exception {
        factory.tryClose(KeyAffinity::close);
    }

    @Override
    public Iterator<V> iterator() {
        return factory.get().iterator();
    }
}
