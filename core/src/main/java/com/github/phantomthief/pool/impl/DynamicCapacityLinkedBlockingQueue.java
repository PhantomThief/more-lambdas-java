package com.github.phantomthief.pool.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

import com.github.phantomthief.util.SimpleRateLimiter;

/**
 * 备注，本类主要是用于给
 * {@link com.github.phantomthief.pool.KeyAffinityExecutor#newSerializingExecutor(IntSupplier, IntSupplier, String)}
 * 提供支持的，并不推荐大家直接使用，未来也可能会随时调整实现和行为
 *
 * @author w.vela
 * Created on 2020-08-19.
 */
public class DynamicCapacityLinkedBlockingQueue<E> implements BlockingQueue<E> {

    private final CapacitySettableLinkedBlockingQueue<E> queue;
    private final IntSupplier capacity;
    private final SimpleRateLimiter rateLimiter;

    /**
     * 可以动态调整 capacity 的 {@link java.util.concurrent.LinkedBlockingQueue}
     * @param capacity 这里为了支持可变化性，所以当返回值 <= 0 时，容量为最大值 {@link Integer#MAX_VALUE}
     *
     * 注意: 当需要声明在 field 内初始化时，建议使用 {@link #lazyDynamicCapacityLinkedBlockingQueue(IntSupplier)}
     */
    public DynamicCapacityLinkedBlockingQueue(IntSupplier capacity) {
        this.capacity = capacity;
        int thisCapacity = capacity.getAsInt();
        this.queue = new CapacitySettableLinkedBlockingQueue<>(thisCapacity <= 0 ? Integer.MAX_VALUE : thisCapacity);
        this.rateLimiter = SimpleRateLimiter.create(1);
    }

    /**
     * 参考构建函数 javadoc
     * 本工具方法构建出来的实例只有在第一次使用时才会初始化资源，可以更安全的在 field 内声明并初始化
     */
    public static <T> BlockingQueue<T> lazyDynamicCapacityLinkedBlockingQueue(IntSupplier capacity) {
        return new LazyBlockingQueue<>(() -> new DynamicCapacityLinkedBlockingQueue<>(capacity));
    }

    private void tryCheckCapacity() {
        if (rateLimiter.tryAcquire()) {
            int thisCapacity = capacity.getAsInt();
            if (thisCapacity <= 0) {
                thisCapacity = Integer.MAX_VALUE;
            }
            if (thisCapacity != queue.getCapacity()) {
                queue.setCapacity(thisCapacity);
            }
        }
    }

    @Override
    public boolean add(E e) {
        tryCheckCapacity();
        return queue.add(e);
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public E remove() {
        return queue.remove();
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        tryCheckCapacity();
        return queue.addAll(c);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    @Override
    public void put(E o) throws InterruptedException {
        tryCheckCapacity();
        queue.put(o);
    }

    @Override
    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        tryCheckCapacity();
        return queue.offer(o, timeout, unit);
    }

    @Override
    public boolean offer(E o) {
        tryCheckCapacity();
        return queue.offer(o);
    }

    @Override
    public E take() throws InterruptedException {
        return queue.take();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public E poll() {
        return queue.poll();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public String toString() {
        return queue.toString();
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return queue.drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        return queue.drainTo(c, maxElements);
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public Spliterator<E> spliterator() {
        return queue.spliterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        queue.forEach(action);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return queue.removeIf(filter);
    }
}
