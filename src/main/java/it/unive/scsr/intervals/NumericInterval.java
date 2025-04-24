package it.unive.scsr.intervals;

import it.unive.lisa.util.numeric.MathNumber;

import java.util.Objects;

public class NumericInterval {

    public sealed interface IntervalNumber {

        Number number();

        /**
         * Converts this {@code IntervalNumber} to a {@code MathNumber}. If the {@code IntervalNumber} is a {@code Long}
         * or {@code Double}, a new {@code MathNumber} is created directly from its primitive value. Otherwise, it's
         * assumed to be a {@code BigDecimal} and a new {@code MathNumber} is created from its {@code BigDecimal} value.
         *
         * @return A new {@code MathNumber} representing the value of this interval.
         */
        default MathNumber toMathNumber() {
            if (this instanceof Long allowed) new MathNumber(allowed.value);
            if (this instanceof Double allowed) new MathNumber(allowed.value);
            return new MathNumber((java.math.BigDecimal) this.number());
        }

        /**
         * Represents an interval number with a {@code long} value.
         *
         * @param value The {@code long} value of the interval.
         */
        record Long(long value) implements IntervalNumber {

            @Override
            public Number number() {
                return value;
            }
        }

        /**
         * Represents an interval number with a {@code double} value.
         *
         * @param value The {@code double} value of the interval.
         */
        record Double(double value) implements IntervalNumber {

            @Override
            public Number number() {
                return value;
            }
        }


        /**
         * Represents an interval number with a {@code BigDecimal} value.
         *
         * @param value The {@code BigDecimal} value of the interval.
         */
        record BigDecimal(java.math.BigDecimal value) implements IntervalNumber {

            @Override
            public Number number() {
                return value;
            }
        }
    }

    public static final NumericInterval INFINITY =
            new NumericInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);

    public final MathNumber low;
    public final MathNumber high;

    private NumericInterval(MathNumber low, MathNumber high) {
        this.low = low;
        this.high = high;
    }

    public NumericInterval(IntervalNumber low, IntervalNumber high) {
        // Force the specified argument to be NonNull.
        Objects.requireNonNull(low);
        Objects.requireNonNull(high);

        this.low = low.toMathNumber();
        this.high = high.toMathNumber();
    }

    public boolean lowIsMinusInfinity() {
        return low.isMinusInfinity();
    }

    public boolean highIsPlusInfinity() {
        return high.isPlusInfinity();
    }

    public boolean isInfinite() {
        return isInfinity() || (highIsPlusInfinity() || lowIsMinusInfinity());
    }

    public boolean isFinite() {
        return !isInfinite();
    }

    public boolean isInfinity() {
        return this == INFINITY;
    }

    public boolean isSingleton() {
        return isFinite() && low.equals(high);
    }

    public boolean is(int n) {
        return isSingleton() && low.is(n);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        NumericInterval that = (NumericInterval) o;
        return low.equals(that.low) && high.equals(that.high);
    }

    @Override
    public int hashCode() {
        int result = low.hashCode();
        result = 31 * result + high.hashCode();
        return result;
    }
}
