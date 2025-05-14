package it.unive.scsr.intervals.numbers;

import java.util.function.Predicate;

import static it.unive.scsr.intervals.numbers.Numeric.ZERO;

public sealed interface SigNum extends IntervalNumber permits Numeric, Infinity {

  SigNum negate();

  default boolean isPositive() {
    return switch (this) {
      case Numeric<?> numeric -> numeric.toDecimal().compareTo(ZERO) > 0;
      case Infinity infinity -> infinity instanceof PlusInfinity;
    };
  }

  default boolean isNegative() {
    return switch (this) {
      case Numeric<?> numeric -> numeric.toDecimal().compareTo(ZERO) < 0;
      case Infinity infinity -> infinity instanceof MinusInfinity;
    };
  }

  default boolean sameSign(SigNum other) {
    // A SignNum is zero when it is neither positive nor negative. This condition will never be
    // valid for Infinity
    // instances.
    Predicate<SigNum> isZero = sigNum -> !(sigNum.isPositive() || sigNum.isNegative());

    var thisZero = isZero.test(this);
    var otherZero = isZero.test(other);

    // If both values are zero, true is returned. If only one of them is zero, false is returned.
    if (thisZero && otherZero) return true;
    if (thisZero != otherZero) return false;

    // Once this point is reached, the only possible elements to consider are Infinity instances or
    // non-zero numeric
    // values.
    return isPositive() ? other.isPositive() : other.isNegative();
  }
}
