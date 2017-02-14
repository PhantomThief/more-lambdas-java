/**
 * 
 */
package com.github.phantomthief.test;

import org.junit.Test;

import com.github.phantomthief.util.MoreIterables;

/**
 * @author w.vela
 */
public class MoreIterablesTest {

    @Test
    public void testIterable() throws Exception {
        MoreIterables.batchClosedRange(2, 15, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedRange(2, 13, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedRange(15, 2, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedRange(13, 2, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedRange(2, 2, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedRange(13, -2, 4).forEach(System.out::println);
        System.out.println("======");
    }

    @Test
    public void testRange() throws Exception {
        MoreIterables.batchClosedSimpleRange(2, 15, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedSimpleRange(2, 13, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedSimpleRange(15, 2, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedSimpleRange(13, 2, 4).forEach(System.out::println);
        System.out.println("======");
        MoreIterables.batchClosedSimpleRange(2, 2, 4).forEach(System.out::println);
        System.out.println("======");
    }
}
