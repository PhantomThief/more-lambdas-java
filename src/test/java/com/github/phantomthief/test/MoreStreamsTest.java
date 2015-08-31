/**
 * 
 */
package com.github.phantomthief.test;

import org.junit.Test;

import com.github.phantomthief.util.MoreStreams;

/**
 * @author w.vela
 */
public class MoreStreamsTest {

    @Test
    public void testRange() throws Exception {
        MoreStreams.longRangeClosed(10, 15).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.longRangeClosed(15, 10).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.intRangeClosed(10, 15).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.intRangeClosed(15, 10).forEach(System.out::println);
        System.out.println("===");
        MoreStreams.intRangeClosed(15, 15).forEach(System.out::println);
    }
}
