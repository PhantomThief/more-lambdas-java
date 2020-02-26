package com.github.phantomthief.util;

import static com.github.phantomthief.util.MorePreconditions.checkOrThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author w.vela
 * Created on 2020-02-26.
 */
class MorePreconditionsTest {

    @Test
    void test() throws IOException {
        checkOrThrow(true, IOException::new, "test");
        IOException e = assertThrows(IOException.class, () -> checkOrThrow(false, IOException::new, "test:%s", "1"));
        assertEquals("test:1", e.getMessage());
        e = assertThrows(IOException.class, () -> checkOrThrow(false, IOException::new, "test"));
        assertEquals("test", e.getMessage());
        e = assertThrows(IOException.class, () -> checkOrThrow(false, IOException::new));
        assertNull(e.getMessage());
    }
}