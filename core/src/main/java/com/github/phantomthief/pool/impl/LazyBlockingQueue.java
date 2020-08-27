package com.github.phantomthief.pool.impl;

import static com.github.phantomthief.util.MoreSuppliers.lazy;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

/**
 * @author w.vela
 * Created on 2020-08-19.
 */
class LazyBlockingQueue<E> implements BlockingQueue<E> {

    private final Supplier<BlockingQueue<E>> factory;

    LazyBlockingQueue(@Nonnull Supplier<BlockingQueue<E>> factory) {
        this.factory = lazy(factory);
    }

    @Override
    public boolean add(E e) {
        return factory.get().add(e);
    }

    @Override
    public boolean offer(E e) {
        return factory.get().offer(e);
    }

    @Override
    public E remove() {
        return factory.get().remove();
    }

    @Override
    public E poll() {
        return factory.get().poll();
    }

    @Override
    public E element() {
        return factory.get().element();
    }

    @Override
    public E peek() {
        return factory.get().peek();
    }

    @Override
    public void put(E e) throws InterruptedException {
        factory.get().put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return factory.get().offer(e, timeout, unit);
    }

    @Override
    public E take() throws InterruptedException {
        return factory.get().take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return factory.get().poll(timeout, unit);
    }

    @Override
    public int remainingCapacity() {
        return factory.get().remainingCapacity();
    }

    @Override
    public boolean remove(Object o) {
        return factory.get().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return factory.get().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return factory.get().addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return factory.get().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return factory.get().retainAll(c);
    }

    @Override
    public void clear() {
        factory.get().clear();
    }

    @Override
    public int size() {
        return factory.get().size();
    }

    @Override
    public boolean isEmpty() {
        return factory.get().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return factory.get().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return factory.get().iterator();
    }

    @Override
    public Object[] toArray() {
        return factory.get().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return factory.get().toArray(a);
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return factory.get().drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return factory.get().drainTo(c, maxElements);
    }

    @Override
    public String toString() {
        return factory.get().toString();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return factory.get().removeIf(filter);
    }

    @Override
    public Spliterator<E> spliterator() {
        return factory.get().spliterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        factory.get().forEach(action);
    }
}
