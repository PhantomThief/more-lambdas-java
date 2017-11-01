package com.github.phantomthief.test;

import static com.github.phantomthief.util.MoreFunctions.catching;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.common.base.Supplier;

/**
 * @author w.vela
 */
class MoreFunctionsTest {

    @Test
    void testTrying() {
        assertTrue(catching(i -> function(i, Exception::new), 1) == null);
        assertTrue(catching(i -> function(i, IllegalArgumentException::new), 1) == null);
        assertTrue(catching(i -> function(i, null), 1).equals("1"));
    }

    private <X extends Throwable> String function(int i, Supplier<X> exception) throws X {
        if (exception != null) {
            X x = exception.get();
            throw x;
        } else {
            return i + "";
        }
    }
}
