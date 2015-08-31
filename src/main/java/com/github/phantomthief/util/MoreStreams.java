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

    public static LongStream longRangeClosed(long from, long to) {
        if (from <= to) {
            return LongStream.rangeClosed(from, to);
        } else {
            return LongStream.rangeClosed(to, from).map(i -> to - i + from);
        }
    }

    public static IntStream intRangeClosed(int from, int to) {
        if (from <= to) {
            return IntStream.rangeClosed(from, to);
        } else {
            return IntStream.rangeClosed(to, from).map(i -> to - i + from);
        }
    }
}
