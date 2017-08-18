/**
 * 
 */
package com.github.phantomthief.tuple;

/**
 * 大家尽量不要用到这个类哦，很蛋疼了。
 * 
 * @author w.vela
 */
public final class FourTuple<A, B, C, D> extends ThreeTuple<A, B, C> {

    public final D fourth;

    /**
     * use {@link Tuple#tuple(Object, Object, Object, Object)} instead
     */
    public FourTuple(final A a, final B b, final C c, final D d) {
        super(a, b, c);
        fourth = d;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ", " + fourth + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (fourth == null ? 0 : fourth.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FourTuple<?, ?, ?, ?> other = (FourTuple<?, ?, ?, ?>) obj;
        if (fourth == null) {
            if (other.fourth != null) {
                return false;
            }
        } else if (!fourth.equals(other.fourth)) {
            return false;
        }
        return true;
    }

    public D getFourth() {
        return fourth;
    }

}
