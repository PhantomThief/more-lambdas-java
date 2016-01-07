/**
 * 
 */
package com.github.phantomthief.tuple;

/**
 * @author w.vela
 * 
 *         多元组的工厂方法
 */
public final class Tuple {

    public static <A, B> TwoTuple<A, B> tuple(final A a, final B b) {
        return new TwoTuple<A, B>(a, b);
    }

    public static <A, B, C> ThreeTuple<A, B, C> tuple(final A a, final B b, final C c) {
        return new ThreeTuple<A, B, C>(a, b, c);
    }

    public static <A, B, C, D> FourTuple<A, B, C, D> tuple(final A a, final B b, final C c,
            final D d) {
        return new FourTuple<A, B, C, D>(a, b, c, d);
    }

    private Tuple() {
        throw new UnsupportedOperationException();
    }

}
