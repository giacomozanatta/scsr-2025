package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class UInt16 extends IntegerChecker {
  @Override
  public Numeric<?> smallestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(0);
  }

  @Override
  public Numeric<?> greatestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(65535);
  }
}
