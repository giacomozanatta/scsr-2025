package it.unive.scsr.intervals.numbers;

import java.util.Objects;

// TODO: handle by respecting limit definitions.
public sealed abstract class Infinity
        implements SigNum
        permits MinusInfinity, PlusInfinity {

    @Override
    public final IntervalNumber add(IntervalNumber other, Computation orElse) {
        if (other instanceof Numeric<?> || equals(other)) return this;
        return orElse == null ? NaN.INSTANCE : orElse.perform(this, other);
    }

    @Override
    public final IntervalNumber subtract(IntervalNumber other, Computation orElse) {
        if (other instanceof Numeric<?>) return this;

        var nan = NaN.INSTANCE;
        if (!(other instanceof Infinity otherInfinity)) return orElse == null ? nan : orElse.perform(this, other);
        return !sameSign(otherInfinity) ? this : orElse == null ? nan : orElse.perform(this, other);
    }

    @Override
    public final IntervalNumber multiply(IntervalNumber other, Computation orElse) {
        if (other.isNaN() || other.isZero()) return orElse == null ? NaN.INSTANCE : orElse.perform(this, other);
        return sameSign((SigNum) other) ? PlusInfinity.INSTANCE : MinusInfinity.INSTANCE;
    }

    @Override
    public final IntervalNumber divide(IntervalNumber other, Computation orElse) {
        if (other instanceof Numeric<?> otherNumeric && !otherNumeric.isZero()) {
            return sameSign(otherNumeric) ? PlusInfinity.INSTANCE : MinusInfinity.INSTANCE;
        }

        // In a division only non-zero numeric values can appear as denominators, otherwise NaN is returned.
        return orElse == null ? NaN.INSTANCE : orElse.perform(this, other);
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
}
