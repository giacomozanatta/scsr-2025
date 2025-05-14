package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class Int8 extends IntegerChecker {
  @Override
  public Numeric<?> smallestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(Byte.MIN_VALUE);
  }

  @Override
  public Numeric<?> greatestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(Byte.MAX_VALUE);
  }
}
