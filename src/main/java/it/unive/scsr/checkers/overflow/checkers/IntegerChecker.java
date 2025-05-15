package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.intervals.numbers.Numeric;

public abstract sealed class IntegerChecker extends SizeChecker
    permits Int16, Int32, Int8, UInt16, UInt32, UInt8 {

  @Override
  public OverflowingLevel isOverflowing(Intervals intervals) {
    // ...
    if (intervals.isBottom()) return OverflowingLevel.base();

    var interval = intervals.interval;
    var min = interval.low;
    var max = interval.high;

    // From this point on the interval is finite, that is, the lower and upper bounds are
    // represented by finite numbers or by infinity.
    var isUnderflow = min.lessThan(smallestNumber());
    var isOverflow = max.greaterThan(greatestNumber());

    if (isUnderflow || isOverflow) {
      // Whenever the value is still outside the limit, the overflow is definite.
      var alwaysDown = max.lessThan(smallestNumber());
      var alwaysUp = min.greaterThan(greatestNumber());

      // If the direction is consistently down or up, return the definite overflow value. Otherwise,
      // return the possible
      // overflow value, accounting for uncertainty.
      if (alwaysDown || alwaysUp) return OverflowingLevel.definite();
      return OverflowingLevel.possible();
    }

    // If no further information can be deduced from the specified ranges, the verifier will not
    // assume anything and
    // will not report any errors.
    return OverflowingLevel.base();
  }

  public abstract Numeric<?> greatestNumber();

  public abstract Numeric<?> smallestNumber();
}
