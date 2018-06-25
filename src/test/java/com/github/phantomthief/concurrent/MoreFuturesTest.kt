package com.github.phantomthief.concurrent

import com.github.phantomthief.concurrent.MoreFutures.getUnchecked
import com.github.phantomthief.concurrent.MoreFutures.tryWait
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors.listeningDecorator
import com.google.common.util.concurrent.UncheckedExecutionException
import com.google.common.util.concurrent.UncheckedTimeoutException
import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds
import java.util.ArrayList
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author w.vela
 * Created on 2018-06-26.
 */
internal class MoreFuturesTest {

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
    @Throws(TryWaitException::class)
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
        for (entry in result.success.entries) {
            val k = entry.key
            val s = entry.value
            assertEquals("test:$k", s)
        }
        result.orThrow()
        result.orThrowUnchecked()
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
        val result = tryWait(futures, ofMillis(100))
        for (future in futures) {
            val s = result.success[future]
            if (s != null) {
                assertTrue(s.startsWith("test:"))
            } else {
                val timeoutException = result.timeout[future]
                assertNotNull(timeoutException)
            }
        }
        assertThrows(TryWaitException::class.java) { result.orThrow() }
        val exception = assertThrows(
            TryWaitUncheckedException::class.java
        ) { result.orThrowUnchecked() }
        assertEquals(1, exception.getTimeout<Any>().size)
        val canceledMap = result.cancelAllTimeout(true)
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
        val result = tryWait(futures, ofSeconds(1))
        for (future in futures) {
            val s = result.success[future]
            if (s != null) {
                assertTrue(s.startsWith("test:"))
            } else {
                val exception: Throwable? = result.failed[future]
                assertSame(exception!!.javaClass, IllegalStateException::class.java)
            }
        }
        val exception = assertThrows(TryWaitException::class.java) { result.orThrow() }
        assertThrows(
            TryWaitUncheckedException::class.java
        ) { result.orThrowUnchecked() }
        assertEquals(1, exception.getFailed<Any>().size)
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
        val result = tryWait(futures, ofMillis(100))
        for (future in futures) {
            val s = result.success[future]
            if (s != null) {
                assertTrue(s.startsWith("test:"))
            } else {
                val exception = result.cancel[future]
                assertNotNull(exception)
            }
        }
        assertThrows(TryWaitException::class.java) { result.orThrow() }
        val exception = assertThrows(
            TryWaitUncheckedException::class.java
        ) { result.orThrowUnchecked() }
        assertEquals(1, exception.getCancel<Any>().size)
    }
}