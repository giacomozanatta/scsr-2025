package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.checkers.OverflowChecker;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

public final class UInt8 extends SizeChecker {
  public UInt8(OverflowChecker.NumericalSize size) {
    super(size);
  }

  @Override
  public Numeric<?> minLimit() {
    return IntervalNumber.ofPrimitiveOrThrow(0);
  }

  @Override
  public Numeric<?> maxLimit() {
    return IntervalNumber.ofPrimitiveOrThrow(255);
  }
}
