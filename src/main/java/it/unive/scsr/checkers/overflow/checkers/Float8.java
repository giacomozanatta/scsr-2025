package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.DecimalNumber;
import it.unive.scsr.intervals.numbers.IntervalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

import java.math.BigDecimal;

public final class Float8 extends DecimalChecker {

  @Override
  public Numeric<?> smallestPositiveNormalNumber() {
    return new DecimalNumber(new BigDecimal("0.015625"));
  }

  @Override
  public Numeric<?> smallestPositiveSubnormalNumber() {
    return new DecimalNumber(new BigDecimal("0.001953125"));
  }

  @Override
  public Numeric<?> largestNumber() {
    return IntervalNumber.ofPrimitiveOrThrow(240);
  }
}
