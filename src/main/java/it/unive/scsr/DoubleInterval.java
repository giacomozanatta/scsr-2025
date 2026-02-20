package it.unive.scsr;

import it.unive.lisa.util.numeric.MathNumber;
import java.util.Objects;

public class DoubleInterval implements Comparable<DoubleInterval> {
    private final MathNumber low;
    private final MathNumber high;

    public static final DoubleInterval ZERO = new DoubleInterval(MathNumber.ZERO, MathNumber.ZERO);
    public static final DoubleInterval INFINITY = new DoubleInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);

    public DoubleInterval(double low, double high) {
        this.low = new MathNumber(low);
        this.high = new MathNumber(high);
    }

    public DoubleInterval(MathNumber low, MathNumber high) {
        this.low = low;
        this.high = high;
    }

    public DoubleInterval(int low, int high) {
        this.low = new MathNumber(low);
        this.high = new MathNumber(high);
    }

    public MathNumber getLow() {
        return low;
    }

    public MathNumber getHigh() {
        return high;
    }

    public boolean includes(DoubleInterval other) {
        return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
    }

    public boolean isInfinity() {
        return low.isMinusInfinity() && high.isPlusInfinity();
    }

    @Override
    public int compareTo(DoubleInterval other) {
        int lowCompare = low.compareTo(other.low);
        if (lowCompare != 0) return lowCompare;
        return high.compareTo(other.high);
    }

    @Override
    public String toString() {
        return "[" + low + ", " + high + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DoubleInterval that = (DoubleInterval) obj;
        return Objects.equals(low, that.low) && Objects.equals(high, that.high);
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }
}