package it.unive.scsr.intervals;

import it.unive.scsr.intervals.numbers.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class NumericInterval implements Comparable<NumericInterval> {

    public static final NumericInterval INFINITY =
            new NumericInterval(MinusInfinity.INSTANCE, PlusInfinity.INSTANCE);

    public final SigNum low;
    public final SigNum high;

    public NumericInterval(SigNum low, SigNum high) {
        // The low parameter must be less than high, and if they are equal, they must not be Infinity instances.
        if (low.greaterThan(high)) throw new IllegalArgumentException();
        if (low.equals(high) && low.isInfinity()) throw new IllegalArgumentException();

        this.low = low;
        this.high = high;
    }

    public NumericInterval(Numeric<?> singletonNumber) {
        this(singletonNumber, singletonNumber);
    }

    public boolean isFinite() {
        return low instanceof Numeric<?> && high instanceof Numeric<?>;
    }

    public boolean isInfinity() {
        return low.isInfinity() && high.isInfinity();
    }

    public boolean isSingleton() {
        return isFinite() && low.equals(high);
    }

    public boolean is(Numeric<?> numeric) {
        return isSingleton() && low.equals(numeric);
    }

    public boolean includes(NumericInterval other) {
        return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
    }

    public Optional<NumericInterval> negate() {
        if (low instanceof SigNum lowSig && high instanceof SigNum highSig) {
            return Optional.of(new NumericInterval(highSig.negate(), lowSig.negate()));
        }

        // If any of the elements is NaN, the optional empty is returned since the interval cannot be negated.
        return Optional.empty();
    }

    public Optional<NumericInterval> intersection(NumericInterval other) {
        // It is assumed, due to the constraints on the constructor, that the lower and upper bounds of the ranges are
        // instances of SigNum.
        SigNum newLower = this.low.max(other.low).asSigNum();
        SigNum newUpper = this.high.min(other.high).asSigNum();

        // If intersection is not possible, an empty optional is returned.
        return newLower.greaterThan(newUpper) ?
                Optional.empty() :
                Optional.of(new NumericInterval(newLower, newUpper));
    }

    public NumericInterval union(NumericInterval other) {
        // There can always be an interval that includes both ranges.
        return new NumericInterval(
                this.low.min(other.low).asSigNum(),
                this.high.max(other.high).asSigNum());
    }

    public Optional<NumericInterval> add(NumericInterval other) {
        // Perform addition between two intervals. The order is somewhat preserved so that the smallest element on the
        // left is added to the smallest element on the right to get the smallest number. The same reasoning applies to
        // get the largest number.
        return buildOrEmpty(this.low.add(other.low), this.high.add(other.high));
    }

    public Optional<NumericInterval> subtract(NumericInterval other) {
        // Perform subtraction between two intervals. The order here is not "linearly" preserved in the sense that to
        // get the smallest element, the lower element of the left interval is related to the larger element of the
        // right interval. For the largest element the reasoning is similar, that is, the largest element of the left
        // interval is related to the smallest element of the right interval.
        return buildOrEmpty(this.low.subtract(other.high), this.high.subtract(other.low));
    }

    public Optional<NumericInterval> multiply(NumericInterval other) {
        // All possible combinations are calculated to obtain the minimum number for the smallest element and the
        // maximum number for the largest element.
        var multiplications =
                List.of(this.low.multiply(other.low),
                        this.low.multiply(other.high),
                        this.high.multiply(other.low),
                        this.high.multiply(other.high));

        // Returns the most accurate approximation for the multiplication operation between two ranges.
        return buildOrEmpty(
                multiplications.stream().min(IntervalNumber::compareTo).orElseThrow(),
                multiplications.stream().max(IntervalNumber::compareTo).orElseThrow());
    }

    public Optional<NumericInterval> divide(NumericInterval other) {
        // All possible combinations are calculated to obtain the minimum number for the smallest element and the
        // maximum number for the largest element.
        var divisions =
                List.of(this.low.divide(other.low),
                        this.low.divide(other.high),
                        this.high.divide(other.low),
                        this.high.divide(other.high));

        // Returns the most accurate approximation for the division operation between two ranges.
        return buildOrEmpty(
                divisions.stream().min(IntervalNumber::compareTo).orElseThrow(),
                divisions.stream().max(IntervalNumber::compareTo).orElseThrow());

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        NumericInterval that = (NumericInterval) o;
        return Objects.equals(low, that.low) && Objects.equals(high, that.high);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(low);
        result = 31 * result + Objects.hashCode(high);
        return result;
    }

    @Override
    public int compareTo(NumericInterval o) {
        int cmp = low.compareTo(o.low);
        return cmp != 0 ? cmp : high.compareTo(o.high);
    }

    private static Optional<NumericInterval> buildOrEmpty(IntervalNumber low, IntervalNumber high) {
        // ...
        try {
            return Optional.of(new NumericInterval(low.asSigNum(), high.asSigNum()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
