package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.Intervals;
import it.unive.scsr.intervals.numbers.Numeric;

public abstract sealed class DecimalChecker extends SizeChecker permits Float16, Float32, Float8 {

  @Override
  public OverflowingLevel isOverflowing(Intervals intervals) {
    // TODO
    return OverflowingLevel.base();
  }

  public abstract Numeric<?> smallestPositiveNormalNumber();

  public abstract Numeric<?> smallestPositiveSubnormalNumber();

  public abstract Numeric<?> largestNumber();
}
