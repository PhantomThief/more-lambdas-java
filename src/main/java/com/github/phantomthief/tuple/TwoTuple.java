package com.github.phantomthief.tuple;

/**
 * @author w.vela
 */
public class TwoTuple<A, B> {

    public final A first;

    public final B second;

    /**
     * use {@link Tuple#tuple(Object, Object)} instead
     */
    @Deprecated
    public TwoTuple(final A a, final B b) {
        first = a;
        second = b;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (first == null ? 0 : first.hashCode());
        result = prime * result + (second == null ? 0 : second.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TwoTuple<?, ?> other = (TwoTuple<?, ?>) obj;
        if (first == null) {
            if (other.first != null) {
                return false;
            }
        } else if (!first.equals(other.first)) {
            return false;
        }
        if (second == null) {
            return other.second == null;
        } else {
            return second.equals(other.second);
        }
    }

}
