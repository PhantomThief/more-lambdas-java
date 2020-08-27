package com.github.phantomthief.pool.impl;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author w.vela
 * Created on 2018-11-29.
 */
class ThreadListeningExecutorService extends ForwardingListeningExecutorService {
    
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ListeningExecutorService wrapped;

    ThreadListeningExecutorService(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.wrapped = listeningDecorator(threadPoolExecutor);
    }

    @Override
    protected ListeningExecutorService delegate() {
        return wrapped;
    }

    public int getActiveCount() {
        return threadPoolExecutor.getActiveCount();
    }

    public int getMaximumPoolSize() {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    public int getQueueSize() {
        return threadPoolExecutor.getQueue().size();
    }

    public int getQueueRemainingCapacity() {
        return threadPoolExecutor.getQueue().remainingCapacity();
    }
}
