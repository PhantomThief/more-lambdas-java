package com.github.phantomthief.concurrent

import com.github.phantomthief.concurrent.MoreFutures.getUnchecked
import com.github.phantomthief.concurrent.MoreFutures.scheduleWithDynamicDelay
import com.github.phantomthief.concurrent.MoreFutures.tryWait
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors.listeningDecorator
import com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination
import com.google.common.util.concurrent.UncheckedExecutionException
import com.google.common.util.concurrent.UncheckedTimeoutException
import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.util.ArrayList
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author w.vela
 * Created on 2018-06-26.
 */
internal class MoreFuturesTest {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val executor = listeningDecorator(newFixedThreadPool(10))

    @Test
    fun testGetUnchecked() {
        val future = executor.submit<String> { "test" }
        assertEquals("test", getUnchecked(future, ofMillis(100)))
        val future2 = executor.submit<String> {
            sleepUninterruptibly(1, SECONDS)
            "test"
        }
        assertThrows(UncheckedTimeoutException::class.java) { getUnchecked(future2, ofMillis(1)) }
        val future3 = executor.submit<String> { throw IllegalStateException("test") }
        val exception = assertThrows(
            UncheckedExecutionException::class.java
        ) { getUnchecked(future3, ofMillis(100)) }
        assertSame(exception.cause!!.javaClass, IllegalStateException::class.java)
    }

    @Test
    fun testTryWait1() {
        val list = (0..2).toList()
        val result =
            tryWait<Int, String, RuntimeException>(
                list,
                ofSeconds(1)
            ) { it ->
                executor.submit<String> {
                    "test:$it"
                }
            }
        for ((k, v) in result) {
            assertEquals("test:$k", v)
        }
    }

    @Test
    fun testTryWaitWithNull() {
        val list = (0..2).toList()
        val result =
            tryWait<Int, String, RuntimeException>(
                list,
                ofSeconds(1)
            ) { it ->
                executor.submit<String?> {
                    null
                }
            }
        for ((_, v) in result) {
            assertNull(v)
        }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun testTryWait2() {
        val futures = ArrayList<ListenableFuture<String>>()
        for (i in 0..2) {
            futures.add(executor.submit<String> { "test:$i" })
        }
        val timeoutFuture = executor.submit<String> {
            SECONDS.sleep(1)
            "timeout."
        }
        futures.add(timeoutFuture)
        val exception =
            assertThrows(TryWaitFutureUncheckedException::class.java) { tryWait(futures, ofMillis(100)) }
        for (future in futures) {
            val s = exception.getSuccess<String>()[future]
            if (s != null) {
                assertTrue(s.startsWith("test:"))
            } else {
                val timeoutException = exception.timeout[future]
                assertNotNull(timeoutException)
            }
        }
        assertEquals(1, exception.timeout.size)
        val canceledMap = exception.cancelAllTimeout(true)
        assertEquals(1, canceledMap.size)
        assertSame(canceledMap.keys.iterator().next(), timeoutFuture)
        assertTrue(canceledMap.values.iterator().next())
        assertThrows(
            CancellationException::class.java
        ) { canceledMap.keys.iterator().next().get() }
    }

    @Test
    fun testTryWait3() {
        val futures = ArrayList<ListenableFuture<String>>()
        for (i in 0..2) {
            futures.add(executor.submit<String> { "test:$i" })
        }
        futures.add(executor.submit<String> { throw IllegalStateException() })
        val result =
            assertThrows(TryWaitFutureUncheckedException::class.java) { tryWait(futures, ofSeconds(1)) }
        for (future in futures) {
            val s = result.getSuccess<String>()[future]
            if (s != null) {
                assertTrue(s.startsWith("test:"))
            } else {
                val exception: Throwable? = result.failed[future]
                assertSame(exception!!.javaClass, IllegalStateException::class.java)
            }
        }
        assertEquals(1, result.failed.size)
    }

    @Test
    fun testTryWait4() {
        val futures = ArrayList<ListenableFuture<String>>()
        for (i in 0..2) {
            futures.add(executor.submit<String> { "test:$i" })
        }
        val futureCancel = executor.submit<String> {
            SECONDS.sleep(1)
            "cancel."
        }
        futures.add(futureCancel)
        futureCancel.cancel(true)
        val result =
            assertThrows(TryWaitFutureUncheckedException::class.java) { tryWait(futures, ofMillis(100)) }
        for (future in futures) {
            val s = result.getSuccess<String>()[future]
            if (s != null) {
                assertTrue(s.startsWith("test:"))
            } else {
                val exception = result.cancel[future]
                assertNotNull(exception)
            }
        }
        assertEquals(1, result.cancel.size)
    }

    @Test
    fun testDynamicDelay() {
        val scheduled = newScheduledThreadPool(100)
        val counter = AtomicInteger()
        val future = scheduleWithDynamicDelay(scheduled, { ofSeconds(3) }) {
            logger.info("current executor count:{}", counter.incrementAndGet())
        }
        sleepUninterruptibly(10, SECONDS)
        assertEquals(3, counter.toInt())
        future.cancel(true)
        sleepUninterruptibly(10, SECONDS)
        assertEquals(3, counter.toInt())
        shutdownAndAwaitTermination(scheduled, 1, DAYS)
    }
}