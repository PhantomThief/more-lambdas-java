/**
 * 
 */
package com.github.phantomthief.util;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * @author w.vela
 */
public class MoreStreams {

    private MoreStreams() {
        throw new UnsupportedOperationException();
    }

    public static LongStream longRangeClosed(long min, long max) {
        if (min <= max) {
            return LongStream.rangeClosed(min, max);
        } else {
            return LongStream.rangeClosed(max, min).map(i -> max - i + min);
        }
    }

    public static IntStream intRangeClosed(int min, int max) {
        if (min <= max) {
            return IntStream.rangeClosed(min, max);
        } else {
            return IntStream.rangeClosed(max, min).map(i -> max - i + min);
        }
    }
}
