package it.unive.scsr.checkers.overflow.checkers;

import it.unive.scsr.intervals.numbers.DecimalNumber;
import it.unive.scsr.intervals.numbers.Numeric;

import java.math.BigDecimal;

public final class Float32 extends DecimalChecker {

  @Override
  public Numeric<?> smallestPositiveNormalNumber() {
    return new DecimalNumber(new BigDecimal(Float.MIN_NORMAL));
  }

  @Override
  public Numeric<?> smallestPositiveSubnormalNumber() {
    return new DecimalNumber(new BigDecimal(Float.MIN_VALUE));
  }

  @Override
  public Numeric<?> largestNumber() {
    return new DecimalNumber(new BigDecimal(Float.MAX_VALUE));
  }
}
