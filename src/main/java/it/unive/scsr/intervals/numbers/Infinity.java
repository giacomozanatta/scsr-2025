package it.unive.scsr.intervals.numbers;

import java.util.Objects;

public sealed abstract class Infinity
        implements SigNum
        permits MinusInfinity, PlusInfinity {

    @Override
    public final IntervalNumber add(IntervalNumber other, Computation orElse) {
        if (other instanceof Numeric<?> || equals(other)) return this;
        return finalize(other, orElse);
    }

    @Override
    public final IntervalNumber subtract(IntervalNumber other, Computation orElse) {
        if (other instanceof Numeric<?>) return this;

        if (!(other instanceof Infinity otherInfinity)) return finalize(other, orElse);
        return !sameSign(otherInfinity) ? this : finalize(other, orElse);
    }

    @Override
    public final IntervalNumber multiply(IntervalNumber other, Computation orElse) {
        if (other.isNaN() || other.isZero()) return finalize(other, orElse);
        return sameSign((SigNum) other) ? PlusInfinity.INSTANCE : MinusInfinity.INSTANCE;
    }

    @Override
    public final IntervalNumber divide(IntervalNumber other, Computation orElse) {
        if (other instanceof Numeric<?> otherNumeric && !otherNumeric.isZero()) {
            return sameSign(otherNumeric) ? PlusInfinity.INSTANCE : MinusInfinity.INSTANCE;
        }

        // In a division only non-zero numeric values can appear as denominators, otherwise NaN is returned.
        return finalize(other, orElse);
    }

    @Override
    public SigNum negate() {
        return switch (this) {
            case PlusInfinity ignored -> MinusInfinity.INSTANCE;
            case MinusInfinity ignored -> PlusInfinity.INSTANCE;
        };
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof Infinity other && sameSign(other);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(this.getClass());
    }

    private IntervalNumber finalize(IntervalNumber other, Computation orElse) {
        var nan = NaN.INSTANCE;
        return orElse != null ? orElse.perform(this, other).orElse(nan) : nan;
    }
}
