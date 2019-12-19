package com.github.phantomthief.tuple;

/**
 * @author w.vela
 */
public final class Tuple {

    private Tuple() {
        throw new UnsupportedOperationException();
    }

    public static <A, B> TwoTuple<A, B> tuple(final A a, final B b) {
        return new TwoTuple<>(a, b);
    }

    public static <A, B, C> ThreeTuple<A, B, C> tuple(final A a, final B b, final C c) {
        return new ThreeTuple<>(a, b, c);
    }

    public static <A, B, C, D> FourTuple<A, B, C, D> tuple(final A a, final B b, final C c,
            final D d) {
        return new FourTuple<>(a, b, c, d);
    }

}
