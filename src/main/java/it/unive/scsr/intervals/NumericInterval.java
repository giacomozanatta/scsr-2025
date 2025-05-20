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
    // The low parameter must be less than high, and if they are equal, they must not be Infinity
    // instances.
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

  public NumericInterval negate() {
    return new NumericInterval(high.negate(), low.negate());
  }

  public Optional<NumericInterval> intersection(NumericInterval other) {
    // It is assumed, due to the constraints on the constructor, that the lower and upper bounds of
    // the ranges are instances of SigNum.
    SigNum newLower = this.low.max(other.low).asSigNum();
    SigNum newUpper = this.high.min(other.high).asSigNum();

    // If intersection is not possible, an empty optional is returned.
    return newLower.greaterThan(newUpper)
        ? Optional.empty()
        : Optional.of(new NumericInterval(newLower, newUpper));
  }

  public NumericInterval union(NumericInterval other) {
    // There can always be an interval that includes both ranges.
    return new NumericInterval(
        this.low.min(other.low).asSigNum(), this.high.max(other.high).asSigNum());
  }

  public Optional<NumericInterval> add(
      NumericInterval other, IntervalNumber.Computation operation) {
    // Perform addition between two intervals. The order is somewhat preserved so that the smallest
    // element on the left is added to the smallest element on the right to get the smallest number.
    // The same reasoning applies to get the largest number.
    return buildOrEmpty(this.low.add(other.low, operation), this.high.add(other.high, operation));
  }

  public Optional<NumericInterval> subtract(
      NumericInterval other, IntervalNumber.Computation operation) {
    // Perform subtraction between two intervals. The order here is not "linearly" preserved in the
    // sense that to get the smallest element, the lower element of the left interval is related to
    // the larger element of the right interval. For the largest element the reasoning is similar,
    // that is, the largest element of the left interval is related to the smallest element of the
    // right interval.
    return buildOrEmpty(
        this.low.subtract(other.high, operation), this.high.subtract(other.low, operation));
  }

  public Optional<NumericInterval> multiply(
      NumericInterval other, IntervalNumber.Computation operation) {
    // All possible combinations are calculated to obtain the minimum number for the smallest
    // element and the maximum number for the largest element.
    var multiplications =
        List.of(
            this.low.multiply(other.low, operation),
            this.low.multiply(other.high, operation),
            this.high.multiply(other.low, operation),
            this.high.multiply(other.high, operation));

    // Returns the most accurate approximation for the multiplication operation between two ranges.
    return buildOrEmpty(
        multiplications.stream().min(IntervalNumber::compareTo).orElseThrow(),
        multiplications.stream().max(IntervalNumber::compareTo).orElseThrow());
  }

  public Optional<NumericInterval> divide(
      NumericInterval other, IntervalNumber.Computation operation) {
    // All possible combinations are calculated to obtain the minimum number for the smallest
    // element and the maximum number for the largest element.
    var divisions =
        List.of(
            this.low.divide(other.low, operation),
            this.low.divide(other.high, operation),
            this.high.divide(other.low, operation),
            this.high.divide(other.high, operation));

    // Returns the most accurate approximation for the division operation between two ranges.
    return buildOrEmpty(
        divisions.stream().min(IntervalNumber::compareTo).orElseThrow(),
        divisions.stream().max(IntervalNumber::compareTo).orElseThrow());
  }

  /**
   * Checks whether this current <code>NumericInterval</code> is definitely less than the specified
   * <code>NumericInterval</code>.
   *
   * @param other <code>NumericInterval</code> to compare with.
   * @return <code>true</code> if this interval is definitely less than the specified interval,
   *     <code>false</code> otherwise.
   */
  public boolean definitelyLessThan(NumericInterval other) {
    return this.high.lessThan(other.low);
  }

  /**
   * Checks whether this current <code>NumericInterval</code> is definitely greater than the
   * specified <code>NumericInterval</code>.
   *
   * @param other <code>NumericInterval</code> to compare with.
   * @return <code>true</code> if this interval is definitely greater than the specified interval,
   *     <code>false</code> otherwise.
   */
  public boolean definitelyGreaterThan(NumericInterval other) {
    return this.low.greaterThan(other.high);
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
  public String toString() {
    return "[" + low + ", " + high + ']';
  }

  @Override
  public int compareTo(NumericInterval o) {
    int cmp = low.compareTo(o.low);
    return cmp != 0 ? cmp : high.compareTo(o.high);
  }

  private static Optional<NumericInterval> buildOrEmpty(IntervalNumber low, IntervalNumber high) {
    // Quick way to create a numeric range, catching any exceptions and returning an empty value if
    // a problem occurs.
    try {
      return Optional.of(new NumericInterval(low.asSigNum(), high.asSigNum()));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
