package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.DecimalNumber;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

import java.math.BigDecimal;

public final class Float16 extends DecimalChecker {

  @Override
  public Numeric<?> smallestPositiveNormalNumber() {
    return new DecimalNumber(new BigDecimal("0.00006103515625"));
  }

  @Override
  public Numeric<?> smallestPositiveSubnormalNumber() {
    return new DecimalNumber(new BigDecimal("0.000000059604644775390625"));
  }

  @Override
  public Numeric<?> largestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(65504);
  }
}
