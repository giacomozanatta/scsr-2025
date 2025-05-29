package it.unive.scsr;
import it.unive.lisa.util.numeric.*;

import java.util.Iterator;

public class CustomIntInterval implements /*Iterable<Long>,*/ Comparable<CustomIntInterval> {
    public static final CustomIntInterval INFINITY = new CustomIntInterval();
    public static final CustomIntInterval ZERO = new CustomIntInterval(0, 0);
    public static final CustomIntInterval ONE = new CustomIntInterval(1, 1);
    public static final CustomIntInterval MINUS_ONE = new CustomIntInterval(-1, -1);
    private final CustomMathNumber low;
    private final CustomMathNumber high;

    private CustomIntInterval() {
        this(CustomMathNumber.MINUS_INFINITY, CustomMathNumber.PLUS_INFINITY);
    }

    public CustomIntInterval(int low, int high) {
        this(new CustomMathNumber((long)low), new CustomMathNumber((long)high));
    }

    public CustomIntInterval(Integer low, Integer high) {
        this(low == null ? CustomMathNumber.MINUS_INFINITY : new CustomMathNumber((long)low), high == null ? CustomMathNumber.PLUS_INFINITY : new CustomMathNumber((long)high));
    }

    public CustomIntInterval(CustomMathNumber low, CustomMathNumber high) {
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

    public CustomMathNumber getHigh() {
        return this.high;
    }

    public CustomMathNumber getLow() {
        return this.low;
    }

    public boolean lowIsMinusInfinity() {
        return this.low.isMinusInfinity();
    }

    public boolean highIsPlusInfinity() {
        return this.high.isPlusInfinity();
    }

    public boolean isInfinite() {
        return this == INFINITY || this.highIsPlusInfinity() || this.lowIsMinusInfinity();
    }

    public boolean isFinite() {
        return !this.isInfinite();
    }

    public boolean isInfinity() {
        return this == INFINITY;
    }

    public boolean isSingleton() {
        return this.isFinite() && this.low.equals(this.high);
    }

    public boolean is(int n) {
        return this.isSingleton() && this.low.is(n);
    }

    private static CustomIntInterval cacheAndRound(CustomIntInterval i) {
        if (i.is(0)) {
            return ZERO;
        } else if (i.is(1)) {
            return ONE;
        } else {
            return i.is(-1) ? MINUS_ONE : new CustomIntInterval(i.low.roundDown(), i.high.roundUp());
        }
    }

    public CustomIntInterval plus(CustomIntInterval other) {
        return !this.isInfinity() && !other.isInfinity() ? cacheAndRound(new CustomIntInterval(this.low.add(other.low), this.high.add(other.high))) : INFINITY;
    }

    public CustomIntInterval diff(CustomIntInterval other) {
        return !this.isInfinity() && !other.isInfinity() ? cacheAndRound(new CustomIntInterval(this.low.subtract(other.high), this.high.subtract(other.low))) : INFINITY;
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

    public CustomIntInterval mul(CustomIntInterval other) {
        if (!this.is(0) && !other.is(0)) {
            if (!this.isInfinity() && !other.isInfinity()) {
                if (this.low.compareTo(CustomMathNumber.ZERO) >= 0 && other.low.compareTo(CustomMathNumber.ZERO) >= 0) {
                    return cacheAndRound(new CustomIntInterval(this.low.multiply(other.low), this.high.multiply(other.high)));
                } else {
                    CustomMathNumber ll = this.low.multiply(other.low);
                    CustomMathNumber lh = this.low.multiply(other.high);
                    CustomMathNumber hl = this.high.multiply(other.low);
                    CustomMathNumber hh = this.high.multiply(other.high);
                    return cacheAndRound(new CustomIntInterval(min(ll, lh, hl, hh), max(ll, lh, hl, hh)));
                }
            } else {
                return INFINITY;
            }
        } else {
            return ZERO;
        }
    }

    public CustomIntInterval div(CustomIntInterval other, boolean ignoreZero, boolean errorOnZero) {
        if (!errorOnZero || !other.is(0) && !other.includes(ZERO)) {
            if (this.is(0)) {
                return ZERO;
            } else if (!other.includes(ZERO)) {
                return this.mul(new CustomIntInterval(CustomMathNumber.ONE.divide(other.high), CustomMathNumber.ONE.divide(other.low)));
            } else if (other.high.isZero()) {
                return this.mul(new CustomIntInterval(CustomMathNumber.MINUS_INFINITY, CustomMathNumber.ONE.divide(other.low)));
            } else if (other.low.isZero()) {
                return this.mul(new CustomIntInterval(CustomMathNumber.ONE.divide(other.high), CustomMathNumber.PLUS_INFINITY));
            } else if (ignoreZero) {
                return this.mul(new CustomIntInterval(CustomMathNumber.ONE.divide(other.low), CustomMathNumber.ONE.divide(other.high)));
            } else {
                CustomIntInterval lower = this.mul(new CustomIntInterval(CustomMathNumber.MINUS_INFINITY, CustomMathNumber.ONE.divide(other.low)));
                CustomIntInterval higher = this.mul(new CustomIntInterval(CustomMathNumber.ONE.divide(other.high), CustomMathNumber.PLUS_INFINITY));
                if (lower.includes(higher)) {
                    return lower;
                } else {
                    return higher.includes(lower) ? higher : cacheAndRound(new CustomIntInterval(lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low, lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
                }
            }
        } else {
            throw new ArithmeticException("CustomIntInterval divide by zero");
        }
    }

    public boolean includes(CustomIntInterval other) {
        return this.low.compareTo(other.low) <= 0 && this.high.compareTo(other.high) >= 0;
    }

    public boolean intersects(CustomIntInterval other) {
        return this.includes(other) || other.includes(this) || this.high.compareTo(other.low) >= 0 && this.high.compareTo(other.high) <= 0 || other.high.compareTo(this.low) >= 0 && other.high.compareTo(this.high) <= 0;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.high == null ? 0 : this.high.hashCode());
        result = 31 * result + (this.low == null ? 0 : this.low.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            CustomIntInterval other = (CustomIntInterval)obj;
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

    /*
    public Iterator<Long> iterator() {
        if (this.low.isFinite() && this.high.isFinite() && !this.low.isNaN() && !this.high.isNaN()) {
            try {
                return new CustomIntIntervalIterator(this.low.toLong(), this.high.toLong());
            } catch (CustomMathNumberConversionException var2) {
                throw new InfiniteIterationException(this);
            }
        } else {
            throw new InfiniteIterationException(this);
        }
    }*/

    public int compareTo(CustomIntInterval o) {
        int cmp;
        return (cmp = this.low.compareTo(o.low)) != 0 ? cmp : this.high.compareTo(o.high);
    }
}
