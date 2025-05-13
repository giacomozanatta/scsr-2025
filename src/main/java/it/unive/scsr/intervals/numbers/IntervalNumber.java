package it.unive.scsr.intervals.numbers;

import it.unive.scsr.utils.Sets;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public sealed interface IntervalNumber extends Comparable<IntervalNumber> permits NaN, SigNum {

  @FunctionalInterface
  interface Computation {
    /**
     * This is a callback that will be called whenever a call to the math operation returns a NaN
     * instance, to be more precise in some cases.
     *
     * @param first The first operand for the binary expression.
     * @param second The second operand for the binary expression.
     * @return A {@link Optional} with a value when the expression is capable of evaluating to
     *     something other than NaN, otherwise it is empty.
     */
    Optional<IntervalNumber> perform(IntervalNumber first, IntervalNumber second);
  }

  /**
   * @return <code>true</code> when this instance is zero, <code>false</code> otherwise.
   */
  default boolean isZero() {
    // Only numeric values that are neither positive nor negative are zero.
    return this instanceof Numeric<?> numeric && !(numeric.isPositive() || numeric.isNegative());
  }

  /**
   * @return <code>true</code> when this instance is <code>Infinity</code>, <code>false</code>
   *     otherwise.
   */
  default boolean isInfinity() {
    return this instanceof Infinity;
  }

  /**
   * @return <code>true</code> when this instance is <code>NaN</code>, <code>false</code> otherwise.
   */
  default boolean isNaN() {
    return this instanceof NaN;
  }

  /**
   * @return this instance as <code>SigNum</code>.
   * @throws ClassCastException when this is not an instance of <code>SigNum</code>.
   */
  default SigNum asSigNum() {
    return (SigNum) this;
  }

  default IntervalNumber min(IntervalNumber other) {
    return Stream.of(this, other).min(IntervalNumber::compareTo).orElseThrow();
  }

  default IntervalNumber max(IntervalNumber other) {
    return Stream.of(this, other).max(IntervalNumber::compareTo).orElseThrow();
  }

  default boolean lessThan(IntervalNumber other) {
    // A negative integer means that this object is less than the specified object.
    return compareTo(other) < 0;
  }

  default boolean greaterThan(IntervalNumber other) {
    // A negative integer means that this object is less than the specified object.
    return compareTo(other) > 0;
  }

  @Override
  default int compareTo(IntervalNumber other) {
    // First, handle the case where the two IntervalNumbers are equal. If they are equal, the
    // comparison result is
    // 0.
    if (equals(other)) return 0;

    // Define a lambda function to compare Numeric IntervalNumbers. This function takes a Numeric
    // IntervalNumber as
    // input and compares its decimal value to the decimal value of the 'other' IntervalNumber. It
    // also handles
    // cases where 'other' is NaN or Infinity.
    Function<Numeric<?>, Integer> numericCompare =
        numeric ->
            switch (other) {
              case NaN ignored -> 1;
              case Infinity infinity -> infinity.isNegative() ? 1 : -1;
              case Numeric<?> otherNumeric ->
                  numeric.toDecimal().compareTo(otherNumeric.toDecimal());
            };

    // Define a lambda function to compare Infinity IntervalNumbers. This function takes an Infinity
    // IntervalNumber
    // as input and compares it to 'other'. It handles cases where 'other' is NaN.
    Function<Infinity, Integer> infinityCompare =
        infinity -> {
          if (infinity.isPositive()) return 1;
          return other instanceof NaN ? 1 : -1;
        };

    return switch (this) {
      case NaN ignored -> -1; // NaN is always considered smaller than any other IntervalNumber.
      case Numeric<?> numeric -> numericCompare.apply(numeric);
      case Infinity infinity -> infinityCompare.apply(infinity);
    };
  }

  default IntervalNumber add(IntervalNumber other) {
    return add(other, null);
  }

  default IntervalNumber subtract(IntervalNumber other) {
    return subtract(other, null);
  }

  default IntervalNumber multiply(IntervalNumber other) {
    return multiply(other, null);
  }

  default IntervalNumber divide(IntervalNumber other) {
    return divide(other, null);
  }

  IntervalNumber add(IntervalNumber other, Computation orElse);

  IntervalNumber subtract(IntervalNumber other, Computation orElse);

  IntervalNumber multiply(IntervalNumber other, Computation orElse);

  IntervalNumber divide(IntervalNumber other, Computation orElse);

  static Optional<Numeric<?>> ofPrimitive(Number constant) {
    // Returns null instead of throwing an exception.
    if (constant == null) return Optional.empty();

    // Definition of continuous and discrete types.
    var continuousTypes = Set.of(Double.class, Float.class);
    var discreteTypes = Set.of(Byte.class, Short.class, Integer.class, Long.class);
    var allowedTypes = Sets.from(continuousTypes, discreteTypes);

    // If the specified number is not assigned, an optional empty value is returned, otherwise the
    // best
    // over-approximation for a discrete or continuous number is returned.
    if (!allowedTypes.contains(constant.getClass())) return Optional.empty();
    return continuousTypes.contains(constant.getClass())
        ? Optional.of(new DecimalNumber(BigDecimal.valueOf(constant.doubleValue())))
        : Optional.of(new IntegerNumber(BigInteger.valueOf(constant.longValue())));
  }

  static Numeric<?> ofPrimitiveOrThrow(Number number) {
    return ofPrimitive(number).orElseThrow();
  }
}
