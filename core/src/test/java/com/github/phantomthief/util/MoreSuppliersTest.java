package com.github.phantomthief.util;

import static com.github.phantomthief.util.MoreSuppliers.lazy;
import static com.github.phantomthief.util.MoreSuppliers.lazyEx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;
import com.github.phantomthief.util.MoreSuppliers.CloseableThrowableSupplier;

/**
 * @author w.vela
 * Created on 2017-02-22.
 */
class MoreSuppliersTest {

    @Test
    void test() {
        int[] counter = { 0 };
        CloseableSupplier<String> supplier = lazy(() -> {
            counter[0]++;
            return "test";
        });
        assertEquals(supplier.get(), "test");
        assertEquals(counter[0], 1);
        supplier.tryClose();
        assertEquals(supplier.get(), "test");
        assertEquals(counter[0], 2);
    }

    @Test
    void tesNull() {
        int[] counter = { 0 };
        CloseableSupplier<String> supplier = lazy(() -> {
            counter[0]++;
            return null;
        });
        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(counter[0], 1);
        supplier.tryClose();
        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(counter[0], 2);
    }

    @Test
    void testEx() {
        int[] counter = {0};
        CloseableThrowableSupplier<String, RuntimeException> supplier = lazyEx(() -> {
            counter[0]++;
            return "test";
        });
        assertEquals(supplier.get(), "test");
        assertEquals(counter[0], 1);
        supplier.tryClose();
        assertEquals(supplier.get(), "test");
        assertEquals(counter[0], 2);
    }

    @Test
    void tesExNull() {
        int[] counter = {0};
        CloseableThrowableSupplier<String, RuntimeException> supplier = lazyEx(() -> {
            counter[0]++;
            return null;
        });
        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(counter[0], 1);
        supplier.tryClose();
        assertNull(supplier.get());
        assertNull(supplier.get());
        assertEquals(counter[0], 2);
    }

    @Test
    void testExError() throws TimeoutException {
        int[] counter = {0};
        CloseableThrowableSupplier<String, TimeoutException> supplier = lazyEx(() -> {
            counter[0]++;
            if (counter[0] == 3) {
                throw new TimeoutException("time out");
            }
            return "test";
        });
        assertEquals("test", supplier.get());
        assertEquals("test", supplier.get());
        assertEquals(counter[0], 1);
        supplier.tryClose();
        assertEquals("test", supplier.get());
        assertEquals("test", supplier.get());
        assertEquals(counter[0], 2);
        supplier.tryClose();
        Assertions.assertThrows(TimeoutException.class, supplier::get);
    }

}