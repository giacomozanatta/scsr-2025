package it.unive.scsr.intervals;

import it.unive.lisa.util.numeric.MathNumber;

import java.math.BigDecimal;

public class NumericInterval implements Comparable<NumericInterval> {

    public sealed interface IntervalNumber {

        Number number();

        /**
         * Converts this {@link IntervalNumber} to a {@link MathNumber}. If the {@link IntervalNumber} is a {@link Long}
         * or {@link Double}, a new {@link MathNumber} is created directly from its primitive value.
         *
         * @return A new {@link MathNumber} representing the value of this interval.
         */
        default MathNumber toMathNumber() {
            if (this instanceof Long allowed) return new MathNumber(allowed.value);
            return new MathNumber((java.lang.Double) this.number());
        }

        /**
         * Converts this {@link IntervalNumber} to a {@link BigDecimal}. If the {@link IntervalNumber} is a {@link Long}
         * or {@link Double}, a new {@link MathNumber} is created directly from its primitive value.
         *
         * @return A new {@link MathNumber} representing the value of this interval.
         */
        default BigDecimal toBigDecimal() {
            if (this instanceof Long allowed) new BigDecimal(allowed.value);
            return BigDecimal.valueOf((java.lang.Double) this.number());
        }

        /**
         * Represents an interval number with a {@code long} value. This is mainly used to over-approximate any discrete
         * number.
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
         * Represents an interval number with a {@code double} value. This is mainly used to over-approximate any
         * continuous number.
         *
         * @param value The {@code double} value of the interval.
         */
        record Double(double value) implements IntervalNumber {

            @Override
            public Number number() {
                return value;
            }
        }
    }

    public static final NumericInterval ZERO = new NumericInterval(MathNumber.ZERO, MathNumber.ZERO);
    public static final NumericInterval INFINITY =
            new NumericInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);

    public final MathNumber low;
    public final MathNumber high;

    public NumericInterval(MathNumber low, MathNumber high) {
        if (low.isNaN() || high.isNaN()) {
            this.low = MathNumber.NaN;
            this.high = MathNumber.NaN;
        } else if (low.compareTo(high) <= 0) {
            this.low = low;
            this.high = high;
        } else {
            this.low = high;
            this.high = low;
        }
    }

    public NumericInterval(IntervalNumber low, IntervalNumber high) {
        this(low.toMathNumber(), high.toMathNumber());
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

    public boolean includes(NumericInterval other) {
        return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
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

    @Override
    public int compareTo(NumericInterval o) {
        int cmp = low.compareTo(o.low);
        return cmp != 0 ? cmp : high.compareTo(o.high);
    }
}
