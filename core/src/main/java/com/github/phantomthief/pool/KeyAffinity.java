package com.github.phantomthief.pool;

/**
 * @author w.vela
 * Created on 2018-02-03.
 */
@Deprecated
public interface KeyAffinity<K, V> extends AutoCloseable, Iterable<V> {

    boolean inited();
}
