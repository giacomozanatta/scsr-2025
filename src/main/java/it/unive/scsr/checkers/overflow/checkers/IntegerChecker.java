package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.intervals.numbers.Numeric;

public abstract sealed class IntegerChecker extends SizeChecker
    permits Int16, Int32, Int8, UInt16, UInt32, UInt8 {

  public static final class IntegerOverflow implements OverflowResult {

    private static final IntegerOverflow EMPTY_INSTANCE = new IntegerOverflow(false, false);

    private final boolean isOverflowing;
    private final boolean definitely;

    public IntegerOverflow(boolean isOverflowing, boolean definitely) {
      this.isOverflowing = isOverflowing;
      this.definitely = definitely;
    }

    @Override
    public boolean isValuable() {
      return isOverflowing;
    }

    @Override
    public boolean isDefinite() {
      return definitely;
    }
  }

  @Override
  public IntegerOverflow isOverflowing(Intervals intervals) {
    // If the given abstract value is bottom, nothing can be said about integer overflow.
    if (intervals.isBottom()) return IntegerOverflow.EMPTY_INSTANCE;

    var interval = intervals.interval;
    var min = interval.low;
    var max = interval.high;

    // Whenever the lower or upper bound of the interval is a decimal number, this checker should
    // not continue analysing.
    if (min.isDecimalNumber() || max.isDecimalNumber()) return IntegerOverflow.EMPTY_INSTANCE;

    // From this point on the interval is finite, that is, the lower and upper bounds are
    // represented by finite numbers or by infinity.
    var isUnderflow = min.lessThan(smallestNumber());
    var isOverflow = max.greaterThan(greatestNumber());

    if (isUnderflow || isOverflow) {
      // Whenever the value is still outside the limit, the overflow is definite.
      var alwaysDown = max.lessThan(smallestNumber());
      var alwaysUp = min.greaterThan(greatestNumber());

      // If the direction is consistently down or up, return the definite overflow value. Otherwise,
      // return the possible overflow value, accounting for uncertainty.
      if (alwaysDown || alwaysUp) return new IntegerOverflow(true, true);
      return new IntegerOverflow(true, false);
    }

    // If no further information can be deduced from the specified ranges, the verifier will not
    // assume anything and will not report any errors.
    return IntegerOverflow.EMPTY_INSTANCE;
  }

  /**
   * Returns the greatest finite number representable by this checker. This method defines the upper
   * bound integer numeric type.
   *
   * @return The greatest finite number.
   */
  public abstract Numeric<?> greatestNumber();

  /**
   * Returns the smallest finite number representable by this checker. This method defines the lower
   * bound integer numeric type.
   *
   * @return The smallest finite number.
   */
  public abstract Numeric<?> smallestNumber();
}
