/**
 * 
 */
package com.github.phantomthief.test;

import java.io.IOException;

import org.junit.Test;

import com.github.phantomthief.util.MoreFunctions;
import com.google.common.base.Supplier;

/**
 * @author w.vela
 */
public class MoreFunctionsTest {

    @Test
    public void testTrying() throws Exception {
        assert(MoreFunctions.catching(i -> function(i, Exception::new), 1) == null);
        assert(MoreFunctions.catching(i -> function(i, IllegalArgumentException::new), 1) == null);
        assert(MoreFunctions.catching(i -> function(i, null), 1).equals("1"));
        try {
            MoreFunctions.catching(i -> function(i, IllegalArgumentException::new), 1,
                    IOException.class);
            assert(false);
        } catch (Throwable e) {
            assert(e.getClass() == IllegalArgumentException.class);
        }
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
