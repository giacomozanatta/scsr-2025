package it.unive.scsr.intervals;

import it.unive.lisa.util.numeric.MathNumber;
import it.unive.scsr.utils.Sets;

import java.util.Optional;
import java.util.Set;

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

        /**
         * Attempts to build an {@link IntervalNumber} from a given {@link Number}. This method checks the runtime type
         * of the input {@link Number} against a set of allowed types. If the type is not allowed, an empty
         * {@link Optional} is returned. If the type is within a set of continuous number types (e.g., Float, Double),
         * an {@link IntervalNumber} wrapping a {@link Double} representation of the input is returned within an
         * {@link Optional}. Otherwise (assuming the type is within the allowed set and not continuous, implying an
         * integral type), an {@link IntervalNumber} wrapping a {@link Long} representation is returned within an
         * {@link Optional}.
         *
         * @param number The {@link Number} to attempt to build an {@link IntervalNumber} from.
         * @return An {@link  Optional} containing the built {@link IntervalNumber} if the input {@link Number}'s type
         * is allowed; otherwise, an empty {@link Optional}.
         */
        static Optional<IntervalNumber> build(Number number) {
            if (!ALLOWED_TYPES.contains(number.getClass())) return Optional.empty();
            if (CONTINUOUS_TYPES.contains(number.getClass())) return Optional.of(new Double(number.doubleValue()));
            return Optional.of(new Long(number.longValue()));
        }
    }

    public static final Set<Class<? extends Number>> CONTINUOUS_TYPES = Set.of(Double.class, Float.class);
    public static final Set<Class<? extends Number>> DISCRETE_TYPES =
            Set.of(Byte.class, Short.class, Integer.class, Long.class);

    public static final Set<Class<? extends Number>> ALLOWED_TYPES = Sets.from(CONTINUOUS_TYPES, DISCRETE_TYPES);

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

    public NumericInterval(IntervalNumber singletonNumber) {
        this(singletonNumber.toMathNumber(), singletonNumber.toMathNumber());
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
