package it.unive.scsr;

import it.unive.lisa.util.numeric.MathNumber;

import java.util.Objects;

public class DoubleInterval implements Comparable<DoubleInterval> {
    private final MathNumber low;
    private final MathNumber high;

    public DoubleInterval(double low, double high) {
        this.low = new MathNumber(low);
        this.high = new MathNumber(high);
    }

    public DoubleInterval(MathNumber low, MathNumber high) {
        this.low = low;
        this.high = high;
    }

    public static final DoubleInterval ZERO = new DoubleInterval(0., 0.);
    public static final DoubleInterval INFINITY = new DoubleInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);


    public MathNumber getLow() {
        return low;
    }

    public MathNumber getHigh() {
        return high;
    }
    
    public boolean isInfinity() {
        return this.equals(DoubleInterval.INFINITY);
    }

    public boolean includes(DoubleInterval other) {
        return this.low.compareTo(other.low) <= 0 && this.high.compareTo(other.high) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DoubleInterval that = (DoubleInterval) o;
        return this.compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }

    @Override
    public int compareTo(
            DoubleInterval o) {
        int cmp;
        if ((cmp = low.compareTo(o.low)) != 0)
            return cmp;
        return high.compareTo(o.high);
    }


    @Override
    public String toString() {
        return "[" + low + ", " + high + "]";
    }
}
