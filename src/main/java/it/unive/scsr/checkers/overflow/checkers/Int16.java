package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class Int16 extends SizeChecker {
  @Override
  public Numeric<?> minLimit() {
    return IntervalNumber.ofPrimitiveOrThrow(Short.MIN_VALUE);
  }

  @Override
  public Numeric<?> maxLimit() {
    return IntervalNumber.ofPrimitiveOrThrow(Short.MAX_VALUE);
  }
}
