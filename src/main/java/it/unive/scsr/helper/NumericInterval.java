package it.unive.scsr.helper;

// import it.unive.lisa.util.numeric.IntInterval;

import it.unive.lisa.util.numeric.MathNumber;

public class NumericInterval implements Comparable<NumericInterval> {

  private final MathNumber low;
  private final MathNumber high;

  public static final NumericInterval ZERO = new NumericInterval(0, 0);
  public static final NumericInterval ONE = new NumericInterval(1, 1);
  public static final NumericInterval MINUS_ONE = new NumericInterval(-1, -1);
  public static final NumericInterval INFINITY = new NumericInterval();

  private static MathNumber toMathNumber(Number n) {
    if (n == null)
      throw new NullPointerException();

    if (n instanceof Double || n instanceof Float)
      return new MathNumber(n.doubleValue());

    return new MathNumber(n.longValue());
  }

  private static MathNumber toMathNumber(Number n, MathNumber df) {
    if (n == null)
      return df;

    if (n instanceof Double || n instanceof Float)
      return new MathNumber(n.doubleValue());

    return new MathNumber(n.longValue());
  }

  private static NumericInterval cacheAndRound(NumericInterval ni) {
    if (ni.is(0))
      return ZERO;
    if (ni.is(1))
      return ONE;
    if (ni.is(-1))
      return MINUS_ONE;
    if (ni.isInfinity())
      return INFINITY;

    return new NumericInterval(ni.low, ni.high);
  }

  private static MathNumber min(MathNumber... nums) {
    if (nums.length == 0)
      throw new IllegalArgumentException("No numbers provided");

    MathNumber min = nums[0];
    for (MathNumber num : nums)
      min = min.min(num);

    return min;
  }

  private static MathNumber max(MathNumber... nums) {
    if (nums.length == 0)
      throw new IllegalArgumentException("No numbers provided");

    MathNumber max = nums[0];
    for (MathNumber num : nums)
      max = max.max(num);

    return max;
  }

  /*
   * +--------------------------+
   * + CONSTRUCTORS
   * +--------------------------+
   */
  private NumericInterval() {
    this(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
  }

  public NumericInterval(double _low, double _high) {
    this(Double.valueOf(_low), Double.valueOf(_high));
  }

  public NumericInterval(long _low, long _high) {
    this(Long.valueOf(_low), Long.valueOf(_high));
  }

  public NumericInterval(Number _low, Number _high) {
    this(toMathNumber(_low, MathNumber.MINUS_INFINITY),
        toMathNumber(_high, MathNumber.PLUS_INFINITY));
  }

  public NumericInterval(MathNumber _low, MathNumber _high) {
    if (_low.isNaN() || _high.isNaN()) {
      this.low = MathNumber.NaN;
      this.high = MathNumber.NaN;
    } else if (_low.compareTo(_high) <= 0) {
      this.low = _low;
      this.high = _high;
    } else {
      this.low = _high;
      this.high = _low;
    }
  }

  public MathNumber getLow() {
    return low;
  }

  public MathNumber getHigh() {
    return high;
  }

  public boolean lowIsMinusInfinity() {
    return low.isMinusInfinity();
  }

  public boolean highIsPlusInfinity() {
    return high.isPlusInfinity();
  }

  public boolean isInfinity() {
    return lowIsMinusInfinity() && highIsPlusInfinity();
  }

  public boolean isInfinite() {
    return lowIsMinusInfinity() || highIsPlusInfinity();
  }

  public boolean isFinite() {
    return !isInfinite();
  }

  public boolean isSingleton() {
    return isFinite() && low.equals(high);
  }

  public boolean is(Number value) {
    return isSingleton() && low.equals(toMathNumber(value));
  }

  /*
   * +--------------------------+
   * + ARITHMETIC
   * +--------------------------+
   */
  public NumericInterval plus(NumericInterval other) {
    if (isInfinity() || other.isInfinity())
      return INFINITY;

    return cacheAndRound(new NumericInterval(low.add(other.low),
        high.add(other.high)));
  }

  public NumericInterval diff(NumericInterval other) {
    if (isInfinity() || other.isInfinity())
      return INFINITY;

    return cacheAndRound(new NumericInterval(low.subtract(other.high),
        high.subtract(other.low)));
  }

  public NumericInterval mul(NumericInterval other) {
    if (is(0) || other.is(0))
      return ZERO;
    else if (isInfinity() || other.isInfinity())
      return INFINITY;

    if (low.compareTo(MathNumber.ZERO) >= 0 && other.low.compareTo(MathNumber.ZERO) >= 0)
      return cacheAndRound(new NumericInterval(low.multiply(other.low), high.multiply(other.high)));

    MathNumber ll = low.multiply(other.low);
    MathNumber lh = low.multiply(other.high);
    MathNumber hl = high.multiply(other.low);
    MathNumber hh = high.multiply(other.high);
    return cacheAndRound(new NumericInterval(min(ll, lh, hl, hh), max(ll, lh, hl, hh)));
  }

  public NumericInterval div(NumericInterval other, boolean ignoreZero, boolean errorOnZero) {

    if (errorOnZero && (other.is(0) || other.includes(ZERO)))
      throw new ArithmeticException("NumericInterval divide by zero");

    if (is(0))
      return ZERO;

    if (!other.includes(ZERO))
      return mul(new NumericInterval(MathNumber.ONE.divide(other.high),
          MathNumber.ONE.divide(other.low)));

    else if (other.high.isZero())
      return mul(new NumericInterval(MathNumber.MINUS_INFINITY,
          MathNumber.ONE.divide(other.low)));

    else if (other.low.isZero())
      return mul(new NumericInterval(MathNumber.ONE.divide(other.high),
          MathNumber.PLUS_INFINITY));

    else if (ignoreZero)
      return mul(new NumericInterval(MathNumber.ONE.divide(other.low),
          MathNumber.ONE.divide(other.high)));

    else {

      NumericInterval lower = mul(new NumericInterval(MathNumber.MINUS_INFINITY,
          MathNumber.ONE.divide(other.low)));

      NumericInterval higher = mul(new NumericInterval(MathNumber.ONE.divide(other.high),
          MathNumber.PLUS_INFINITY));

      if (lower.includes(higher))
        return lower;

      else if (higher.includes(lower))
        return higher;

      else
        return cacheAndRound(new NumericInterval(
            lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low,
            lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
    }
  }

  public boolean includes(NumericInterval other) {
    return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
  }

  public boolean intersects(NumericInterval other) {
    MathNumber maxLow = low.compareTo(other.low) >= 0 ? low : other.low;
    MathNumber minHigh = high.compareTo(other.high) <= 0 ? high : other.high;
    return maxLow.compareTo(minHigh) <= 0;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((high == null) ? 0 : high.hashCode());
    result = prime * result + ((low == null) ? 0 : low.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    NumericInterval other = (NumericInterval) obj;

    return java.util.Objects.equals(low, other.low) &&
        java.util.Objects.equals(high, other.high);
  }

  @Override
  public String toString() {
    return "[" + low + ", " + high + "]";
  }

  @Override
  public int compareTo(NumericInterval o) {
    int cmp = low.compareTo(o.low);
    if (cmp != 0)
      return cmp;
    return high.compareTo(o.high);
  }
}