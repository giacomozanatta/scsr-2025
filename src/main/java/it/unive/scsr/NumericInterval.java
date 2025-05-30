package it.unive.scsr;

import it.unive.lisa.util.numeric.MathNumber;

import java.util.Objects;

public class NumericInterval {
    private final MathNumber low;
    private final MathNumber high;

    public static final NumericInterval INFINITY = new NumericInterval(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
    public static final NumericInterval ZERO = new NumericInterval(MathNumber.ZERO, MathNumber.ZERO);

    public static final NumericInterval ONE = new NumericInterval(MathNumber.ONE, MathNumber.ONE);

    public static final NumericInterval MINUS_ONE = new NumericInterval(MathNumber.MINUS_ONE, MathNumber.MINUS_ONE);

    public NumericInterval() {
        this(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
    }

    public NumericInterval(MathNumber low, MathNumber high) {
        if (low.isNaN() || high.isNaN()) {
            this.low = MathNumber.NaN;
            this.high = MathNumber.NaN;
        } else if (low.compareTo(high) <= 0) {
            this.low = low;
            this.high = high;
        } else {
            this.low = high;
            this.high = low;
        }
    }

    public NumericInterval(double low, double high) {
        this.low = new MathNumber(low);
        this.high = new MathNumber(high);
    }

    public NumericInterval(int low, int high) {
        this.low = new MathNumber(low);
        this.high = new MathNumber(high);
    }

    public MathNumber getLow() { return low; }
    public MathNumber getHigh() { return high; }

    public boolean highIsPlusInfinity() {
        return high.isPlusInfinity();
    }

    public boolean lowIsMinusInfinity() {
        return low.isMinusInfinity();
    }

    public boolean isFinite() {
        return !isInfinite();
    }

    public boolean isZero() { return this == ZERO; }

    public boolean isInfinite() {
        return this == INFINITY || (highIsPlusInfinity() || lowIsMinusInfinity());
    }

    public boolean isInfinity() {
        return this == INFINITY;
    }

    public boolean isSingleton() {
        return isFinite() && low.equals(high);
    }

    public boolean is(int n) {
        return isSingleton() && low.is(n);
    }

    public NumericInterval add(NumericInterval other) {
        if (isInfinity() || other.isInfinity())
            return INFINITY;

        return new NumericInterval(this.low.add(other.low), this.high.add(other.high));
    }

    public NumericInterval sub(NumericInterval other) {
        if (isInfinity() || other.isInfinity())
            return INFINITY;

        return new NumericInterval(this.low.subtract(other.high), this.high.subtract(other.low));
    }

    private static MathNumber min(
            MathNumber... nums) {
        if (nums.length == 0)
            throw new IllegalArgumentException("No numbers provided");

        MathNumber min = nums[0];
        for (int i = 1; i < nums.length; i++)
            min = min.min(nums[i]);

        return min;
    }

    private static MathNumber max(
            MathNumber... nums) {
        if (nums.length == 0)
            throw new IllegalArgumentException("No numbers provided");

        MathNumber max = nums[0];
        for (int i = 1; i < nums.length; i++)
            max = max.max(nums[i]);

        return max;
    }

    public NumericInterval mul(NumericInterval other) {
        if (is(0) || other.is(0))
            return ZERO;
        if (isInfinity() || other.isInfinity())
            return INFINITY;

        if (low.compareTo(MathNumber.ZERO) >= 0 && other.low.compareTo(MathNumber.ZERO) >= 0)
            return new NumericInterval(low.multiply(other.low), high.multiply(other.high));

        MathNumber[] products = new MathNumber[] {
                this.low.multiply(other.low), this.low.multiply(other.high),
                this.high.multiply(other.low), this.high.multiply(other.high)
        };
        return new NumericInterval(min(products), max(products));
    }

    public NumericInterval div(NumericInterval other, boolean ignoreZero, boolean errorOnZero) {
        // Throw if errorOnZero and divisor interval contains zero
        if (errorOnZero && (other.includesZero())) {
            throw new ArithmeticException("Division by interval containing zero");
        }

        // If numerator interval is zero
        if (this.isZero()) {
            return ZERO; // assuming ZERO is NumericInterval singleton [0,0]
        }

        if (!other.includesZero()) {
            NumericInterval reciprocal = new NumericInterval(
                    MathNumber.ONE.divide(other.getHigh()),  // 1 / high
                    MathNumber.ONE.divide(other.getLow())    // 1 / low
            );
            return this.mul(reciprocal);

        } else if (other.getHigh().isZero()) {
            NumericInterval reciprocal = new NumericInterval(
                    MathNumber.MINUS_INFINITY,             // -∞
                    MathNumber.ONE.divide(other.getLow())    // 1 / low
            );
            return this.mul(reciprocal);

        } else if (other.getLow().isZero()) {
            NumericInterval reciprocal = new NumericInterval(
                    MathNumber.ONE.divide(other.getHigh()),  // 1 / high
                    MathNumber.PLUS_INFINITY               // +∞
            );
            return this.mul(reciprocal);

        } else if (ignoreZero) {
            NumericInterval reciprocal = new NumericInterval(
                    MathNumber.ONE.divide(other.getLow()),   // 1 / low
                    MathNumber.ONE.divide(other.getHigh())   // 1 / high
            );
            return this.mul(reciprocal);

        } else {
            NumericInterval lower = this.mul(new NumericInterval(
                    MathNumber.MINUS_INFINITY,
                    MathNumber.ONE.divide(other.getLow())
            ));
            NumericInterval higher = this.mul(new NumericInterval(
                    MathNumber.ONE.divide(other.getHigh()),
                    MathNumber.PLUS_INFINITY
            ));

            if (lower.includes(higher))
                return lower;
            else if (higher.includes(lower))
                return higher;
            else
                return new NumericInterval(
                        min(lower.getLow(), higher.getLow()),
                        max(lower.getHigh(), higher.getHigh())
                );
        }
    }

    public boolean includesZero() {
        return low.leq(MathNumber.ZERO) && high.geq(MathNumber.ZERO);
    }

    public boolean includes(NumericInterval other) {
        return this.low.leq(other.low) && this.high.geq(other.high);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        NumericInterval that = (NumericInterval) o;

        if (low == null) {
            if (that.low != null)
                return false;
        } else if (!low.equals(that.low))
            return false;

        if (high == null) {
            if (that.high != null)
                return false;
        } else if (!high.equals(that.high))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }

    @Override
    public String toString() {
        return "NumericInterval{" +
                "low=" + low +
                ", high=" + high +
                '}';
    }

    public int compareTo(
            NumericInterval o) {
        int cmp;
        if ((cmp = low.compareTo(o.low)) != 0)
            return cmp;
        return high.compareTo(o.high);
    }
}
