package it.unive.scsr;

import it.unive.lisa.util.numeric.MathNumber;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * An interval [low, high] backed by {@link MathNumber}, supporting both
 * integer and floating-point bounds via BigDecimal construction.
 */
public class DoubleInterval implements Comparable<DoubleInterval> {

    private final MathNumber low;
    private final MathNumber high;

    public static final DoubleInterval ZERO     = new DoubleInterval(0, 0);
    public static final DoubleInterval INFINITY = new DoubleInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);

    public DoubleInterval(MathNumber low, MathNumber high) {
        this.low  = low;
        this.high = high;
    }

    public DoubleInterval(int low, int high) {
        this(new MathNumber(new BigDecimal(low)), new MathNumber(new BigDecimal(high)));
    }

    public DoubleInterval(double low, double high) {
        this(new MathNumber(new BigDecimal(low)), new MathNumber(new BigDecimal(high)));
    }

    public MathNumber getLow()  { return low; }
    public MathNumber getHigh() { return high; }

    public boolean isInfinity() {
        return low.isMinusInfinity() && high.isPlusInfinity();
    }

    public boolean includes(DoubleInterval other) {
        return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
    }

    @Override
    public int compareTo(DoubleInterval o) {
        int cmp = low.compareTo(o.low);
        return cmp != 0 ? cmp : high.compareTo(o.high);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleInterval)) return false;
        return compareTo((DoubleInterval) o) == 0;
    }

    @Override
    public int hashCode() { return Objects.hash(low, high); }

    @Override
    public String toString() { return "[" + low + ", " + high + "]"; }
}