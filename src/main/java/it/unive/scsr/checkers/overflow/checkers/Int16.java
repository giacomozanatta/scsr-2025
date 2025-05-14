package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class Int16 extends IntegerChecker {
  @Override
  public Numeric<?> smallestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(Short.MIN_VALUE);
  }

  @Override
  public Numeric<?> greatestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(Short.MAX_VALUE);
  }
}
