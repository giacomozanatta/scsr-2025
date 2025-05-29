package it.unive.scsr;//package it.unive.lisa.util.numeric;

import it.unive.lisa.util.numeric.*;

import java.util.Iterator;

public class FloatInterval implements Comparable<FloatInterval> {
    public static final FloatInterval INFINITY = new FloatInterval();
    public static final FloatInterval ZERO = new FloatInterval(0, 0);
    public static final FloatInterval ONE = new FloatInterval(1, 1);
    public static final FloatInterval MINUS_ONE = new FloatInterval(-1, -1);

    private final CustomMathNumber low;
    private final CustomMathNumber high;

    private FloatInterval() {
        this(CustomMathNumber.MINUS_INFINITY, CustomMathNumber.PLUS_INFINITY);
    }

    public FloatInterval(float low, float high) {
        this(new CustomMathNumber(low), new CustomMathNumber(high));
    }

    public FloatInterval(Float low, Float high) {
        this(low == null ? CustomMathNumber.MINUS_INFINITY : new CustomMathNumber(low),
                high == null ? CustomMathNumber.PLUS_INFINITY : new CustomMathNumber(high));
    }

    public FloatInterval(CustomMathNumber low, CustomMathNumber high) {
        if (!low.isNaN() && !high.isNaN()) {
            if (low.compareTo(high) <= 0) {
                this.low = low;
                this.high = high;
            } else {
                this.low = high;
                this.high = low;
            }
        } else {
            this.low = CustomMathNumber.NaN;
            this.high = CustomMathNumber.NaN;
        }
    }

    public CustomMathNumber getLow() {
        return low;
    }

    public CustomMathNumber getHigh() {
        return high;
    }

    public boolean lowIsMinusInfinity() {
        return this.low.isMinusInfinity();
    }

    public boolean highIsPlusInfinity() {
        return this.high.isPlusInfinity();
    }

    public boolean isFinite() {
        return !isInfinite();
    }

    public boolean isInfinite() {
        return this == INFINITY || low.isMinusInfinity() || high.isPlusInfinity();
    }

    public boolean isInfinity() {
        return this == INFINITY;
    }

    public boolean isSingleton() {
        return this.isFinite() && this.low.equals(this.high);
    }

    public boolean is(float val) {
        return isSingleton() && low.is((int) val);
    }

    private static FloatInterval cacheAndRound(FloatInterval i) {
        if (i.is(0)) {
            return ZERO;
        } else if (i.is(1)) {
            return ONE;
        } else {
            return i.is(-1) ? MINUS_ONE : new FloatInterval(i.low.roundDown(), i.high.roundUp());
        }
    }

    public FloatInterval plus(FloatInterval other) {
        return !this.isInfinity() && !other.isInfinity() ? cacheAndRound(new FloatInterval(this.low.add(other.low), this.high.add(other.high))) : INFINITY;
    }

    public FloatInterval diff(FloatInterval other) {
        return !this.isInfinity() && !other.isInfinity() ? cacheAndRound(new FloatInterval(this.low.subtract(other.high), this.high.subtract(other.low))) : INFINITY;
    }

    private static CustomMathNumber min(CustomMathNumber... nums) {
        if (nums.length == 0) {
            throw new IllegalArgumentException("No numbers provided");
        } else {
            CustomMathNumber min = nums[0];

            for(int i = 1; i < nums.length; ++i) {
                min = min.min(nums[i]);
            }

            return min;
        }
    }

    private static CustomMathNumber max(CustomMathNumber... nums) {
        if (nums.length == 0) {
            throw new IllegalArgumentException("No numbers provided");
        } else {
            CustomMathNumber max = nums[0];

            for(int i = 1; i < nums.length; ++i) {
                max = max.max(nums[i]);
            }

            return max;
        }
    }

    public FloatInterval mul(FloatInterval other) {
        if (!this.is(0) && !other.is(0)) {
            if (!this.isInfinity() && !other.isInfinity()) {
                if (this.low.compareTo(CustomMathNumber.ZERO) >= 0 && other.low.compareTo(CustomMathNumber.ZERO) >= 0) {
                    return cacheAndRound(new FloatInterval(this.low.multiply(other.low), this.high.multiply(other.high)));
                } else {
                    CustomMathNumber ll = this.low.multiply(other.low);
                    CustomMathNumber lh = this.low.multiply(other.high);
                    CustomMathNumber hl = this.high.multiply(other.low);
                    CustomMathNumber hh = this.high.multiply(other.high);
                    return cacheAndRound(new FloatInterval(min(ll, lh, hl, hh), max(ll, lh, hl, hh)));
                }
            } else {
                return INFINITY;
            }
        } else {
            return ZERO;
        }
    }

    public FloatInterval div(FloatInterval other, boolean ignoreZero, boolean errorOnZero) {
        if (!errorOnZero || !other.is(0) && !other.includes(ZERO)) {
            if (this.is(0)) {
                return ZERO;
            } else if (!other.includes(ZERO)) {
                return this.mul(new FloatInterval(CustomMathNumber.ONE.divide(other.high), CustomMathNumber.ONE.divide(other.low)));
            } else if (other.high.isZero()) {
                return this.mul(new FloatInterval(CustomMathNumber.MINUS_INFINITY, CustomMathNumber.ONE.divide(other.low)));
            } else if (other.low.isZero()) {
                return this.mul(new FloatInterval(CustomMathNumber.ONE.divide(other.high), CustomMathNumber.PLUS_INFINITY));
            } else if (ignoreZero) {
                return this.mul(new FloatInterval(CustomMathNumber.ONE.divide(other.low), CustomMathNumber.ONE.divide(other.high)));
            } else {
                FloatInterval lower = this.mul(new FloatInterval(CustomMathNumber.MINUS_INFINITY, CustomMathNumber.ONE.divide(other.low)));
                FloatInterval higher = this.mul(new FloatInterval(CustomMathNumber.ONE.divide(other.high), CustomMathNumber.PLUS_INFINITY));
                if (lower.includes(higher)) {
                    return lower;
                } else {
                    return higher.includes(lower) ? higher : cacheAndRound(new FloatInterval(lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low, lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
                }
            }
        } else {
            throw new ArithmeticException("IntInterval divide by zero");
        }
    }

    public boolean includes(FloatInterval other) {
        return this.low.compareTo(other.low) <= 0 && this.high.compareTo(other.high) >= 0;
    }

    public boolean intersects(FloatInterval other) {
        return this.includes(other) || other.includes(this) || this.high.compareTo(other.low) >= 0 && this.high.compareTo(other.high) <= 0 || other.high.compareTo(this.low) >= 0 && other.high.compareTo(this.high) <= 0;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.high == null ? 0 : this.high.hashCode());
        result = 31 * result + (this.low == null ? 0 : this.low.hashCode());
        return result;
    }

    /*
    public Iterator<Long> iterator() {
        if (this.low.isFinite() && this.high.isFinite() && !this.low.isNaN() && !this.high.isNaN()) {
            try {
                return new FloatIntervalIterator(this.low.toLong(), this.high.toLong());
            } catch (CustomMathNumberConversionException var2) {
                throw new InfiniteIterationException(this);
            }
        } else {
            throw new InfiniteIterationException(this);
        }
    }*/

    public int compareTo(FloatInterval o) {
        int cmp;
        return (cmp = this.low.compareTo(o.low)) != 0 ? cmp : this.high.compareTo(o.high);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            FloatInterval other = (FloatInterval)obj;
            if (this.high == null) {
                if (other.high != null) {
                    return false;
                }
            } else if (!this.high.equals(other.high)) {
                return false;
            }

            if (this.low == null) {
                if (other.low != null) {
                    return false;
                }
            } else if (!this.low.equals(other.low)) {
                return false;
            }

            return true;
        }
    }

    public String toString() {
        return "[" + this.low + ", " + this.high + "]";
    }
}
