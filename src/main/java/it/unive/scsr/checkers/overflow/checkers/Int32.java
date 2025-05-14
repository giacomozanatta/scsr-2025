package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class Int32 extends IntegerChecker {
  @Override
  public Numeric<?> smallestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(Integer.MIN_VALUE);
  }

  @Override
  public Numeric<?> greatestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(Integer.MAX_VALUE);
  }
}
