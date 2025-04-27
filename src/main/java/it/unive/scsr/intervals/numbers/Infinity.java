package it.unive.scsr.intervals.numbers;

import java.util.Objects;

public sealed abstract class Infinity
        implements IntervalNumber, SigNum
        permits MinusInfinity, PlusInfinity {

    public Infinity reverse() {
        return switch (this) {
            case PlusInfinity ignored -> MinusInfinity.INSTANCE;
            case MinusInfinity ignored -> PlusInfinity.INSTANCE;
        };
    }

    @Override
    public final IntervalNumber add(IntervalNumber other) {
        if (other instanceof Numeric<?> || equals(other)) return this;
        return NaN.INSTANCE;
    }

    @Override
    public final IntervalNumber subtract(IntervalNumber other) {
        if (other instanceof Numeric<?>) return this;
        if (!(other instanceof Infinity otherInfinity)) return NaN.INSTANCE;
        return !sameSign(otherInfinity) ? this : NaN.INSTANCE;
    }

    @Override
    public final IntervalNumber multiply(IntervalNumber other) {
        if (other instanceof NaN || other.isZero()) return NaN.INSTANCE;
        return sameSign((SigNum) other) ? PlusInfinity.INSTANCE : MinusInfinity.INSTANCE;
    }

    @Override
    public final IntervalNumber divide(IntervalNumber other) {
        if (other instanceof Numeric<?> otherNumeric && !otherNumeric.isZero()) {
            return sameSign(otherNumeric) ? PlusInfinity.INSTANCE : MinusInfinity.INSTANCE;
        }

        // In a division only non-zero numeric values can appear as denominators, otherwise NaN is returned.
        return NaN.INSTANCE;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj instanceof Infinity other && sameSign(other);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(this.getClass());
    }
}
