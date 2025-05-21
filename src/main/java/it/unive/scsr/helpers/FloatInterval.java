package it.unive.scsr.helpers;

import it.unive.lisa.util.numeric.IntIntervalIterator;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;

import java.math.BigDecimal;
import java.util.Iterator;

/**
 * An interval with integer bounds.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FloatInterval implements Iterable<Long>, Comparable<FloatInterval> {

    /**
     * The interval {@code [-Inf, +Inf]}.
     */
    public static final FloatInterval INFINITY = new FloatInterval();

    /**
     * The interval {@code [0, 0]}.
     */
    public static final FloatInterval ZERO = new FloatInterval(0, 0);

    /**
     * The interval {@code [1, 1]}.
     */
    public static final FloatInterval ONE = new FloatInterval(1, 1);

    /**
     * The interval {@code [-1, -1]}.
     */
    public static final FloatInterval MINUS_ONE = new FloatInterval(-1, -1);

    private final MathNumber low;

    private final MathNumber high;

    private FloatInterval() {
        this(MathNumber.MINUS_INFINITY, MathNumber.PLUS_INFINITY);
    }

    /**
     * Builds a new interval. Order of the bounds is adjusted (i.e., if
     * {@code low} is greater then {@code high}, then the interval
     * {@code [high, low]} is created).
     *
     * @param low  the lower bound
     * @param high the upper bound
     */
    public FloatInterval(
            float low,
            float high) {
        this(new MathNumber(low), new MathNumber(high));
    }

    /**
     * Builds a new interval. Order of the bounds is adjusted (i.e., if
     * {@code low} is greater then {@code high}, then the interval
     * {@code [high, low]} is created).
     *
     * @param low  the lower bound (if {@code null}, -inf will be used)
     * @param high the upper bound (if {@code null}, +inf will be used)
     */
    public FloatInterval(
            Float low,
            Float high) {
        this(low == null ? MathNumber.MINUS_INFINITY : new MathNumber(low),
                high == null ? MathNumber.PLUS_INFINITY : new MathNumber(high));
    }

    /**
     * Builds a new interval. Order of the bounds is adjusted (i.e., if
     * {@code low} is greater then {@code high}, then the interval
     * {@code [high, low]} is created).
     *
     * @param low  the lower bound
     * @param high the upper bound
     */
    public FloatInterval(
            MathNumber low,
            MathNumber high) {
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

    /**
     * Yields the upper bound of this interval.
     *
     * @return the upper bound of this interval
     */
    public MathNumber getHigh() {
        return high;
    }

    /**
     * Yields the lower bound of this interval.
     *
     * @return the lower bound of this interval
     */
    public MathNumber getLow() {
        return low;
    }

    /**
     * Yields {@code true} if the lower bound of this interval is set to minus
     * infinity.
     *
     * @return {@code true} if that condition holds
     */
    public boolean lowIsMinusInfinity() {
        return low.isMinusInfinity();
    }

    /**
     * Yields {@code true} if the upper bound of this interval is set to plus
     * infinity.
     *
     * @return {@code true} if that condition holds
     */
    public boolean highIsPlusInfinity() {
        return high.isPlusInfinity();
    }

    /**
     * Yields {@code true} if this is interval is not finite, that is, if at
     * least one bound is set to infinity.
     *
     * @return {@code true} if that condition holds
     */
    public boolean isInfinite() {
        return this == INFINITY || (highIsPlusInfinity() || lowIsMinusInfinity());
    }

    /**
     * Yields {@code true} if this is interval is finite, that is, if neither
     * bound is set to infinity.
     *
     * @return {@code true} if that condition holds
     */
    public boolean isFinite() {
        return !isInfinite();
    }

    /**
     * Yields {@code true} if this is the interval representing infinity, that
     * is, {@code [-Inf, +Inf]}.
     *
     * @return {@code true} if that condition holds
     */
    public boolean isInfinity() {
        return this == INFINITY;
    }

    /**
     * Yields {@code true} if this is a singleton interval, that is, if the
     * lower bound and the upper bound are the same.
     *
     * @return {@code true} if that condition holds
     */
    public boolean isSingleton() {
        return isFinite() && low.equals(high);
    }

    /**
     * Yields {@code true} if this is a singleton interval containing only
     * {@code n}.
     *
     * @param n the integer to test
     *
     * @return {@code true} if that condition holds
     */
    public boolean is(
            float n) {
        return isSingleton() && low != null && low.equals(new BigDecimal(n));
    }

    private static FloatInterval cache(
            FloatInterval i) {
        if (i.is(0))
            return ZERO;
        if (i.is(1))
            return ONE;
        if (i.is(-1))
            return MINUS_ONE;
        return new FloatInterval(i.low, i.high);
    }

    /**
     * Performs the interval addition between {@code this} and {@code other}.
     *
     * @param other the other interval
     *
     * @return {@code this + other}
     */
    public FloatInterval plus(
            FloatInterval other) {
        if (isInfinity() || other.isInfinity())
            return INFINITY;

        return cache(new FloatInterval(low.add(other.low), high.add(other.high)));
    }

    /**
     * Performs the interval subtraction between {@code this} and {@code other}.
     *
     * @param other the other interval
     *
     * @return {@code this - other}
     */
    public FloatInterval diff(
            FloatInterval other) {
        if (isInfinity() || other.isInfinity())
            return INFINITY;

        return cache(new FloatInterval(low.subtract(other.high), high.subtract(other.low)));
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

    /**
     * Performs the interval multiplication between {@code this} and
     * {@code other}.
     *
     * @param other the other interval
     *
     * @return {@code this * other}
     */
    public FloatInterval mul(
            FloatInterval other) {
        if (is(0) || other.is(0))
            return ZERO;
        if (isInfinity() || other.isInfinity())
            return INFINITY;

        if (low.compareTo(MathNumber.ZERO) >= 0 && other.low.compareTo(MathNumber.ZERO) >= 0)
            return cache(new FloatInterval(low.multiply(other.low), high.multiply(other.high)));

        MathNumber ll = low.multiply(other.low);
        MathNumber lh = low.multiply(other.high);
        MathNumber hl = high.multiply(other.low);
        MathNumber hh = high.multiply(other.high);
        return cache(new FloatInterval(min(ll, lh, hl, hh), max(ll, lh, hl, hh)));
    }

    /**
     * Performs the interval division between {@code this} and {@code other}.
     *
     * @param other       the other interval
     * @param ignoreZero  if {@code true}, causes the division to ignore the
     *                        fact that {@code other} might contain 0, producing
     *                        a smaller result
     * @param errorOnZero whether or not an {@link ArithmeticException} should
     *                        be thrown immediately if {@code other} contains
     *                        zero
     *
     * @return {@code this / other}
     *
     * @throws ArithmeticException if {@code other} contains 0 and
     *                                 {@code errorOnZero} is set to
     *                                 {@code true}
     */
    public FloatInterval div(
            FloatInterval other,
            boolean ignoreZero,
            boolean errorOnZero) {
        if (errorOnZero && (other.is(0) || other.includes(ZERO)))
            throw new ArithmeticException("FloatInterval divide by zero");

        if (is(0))
            return ZERO;

        if (!other.includes(ZERO))
            return mul(new FloatInterval(MathNumber.ONE.divide(other.high), MathNumber.ONE.divide(other.low)));
        else if (other.high.isZero())
            return mul(new FloatInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
        else if (other.low.isZero())
            return mul(new FloatInterval(MathNumber.ONE.divide(other.high), MathNumber.PLUS_INFINITY));
        else if (ignoreZero)
            return mul(new FloatInterval(MathNumber.ONE.divide(other.low), MathNumber.ONE.divide(other.high)));
        else {
            FloatInterval lower = mul(new FloatInterval(MathNumber.MINUS_INFINITY, MathNumber.ONE.divide(other.low)));
            FloatInterval higher = mul(new FloatInterval(MathNumber.ONE.divide(other.high), MathNumber.PLUS_INFINITY));

            if (lower.includes(higher))
                return lower;
            else if (higher.includes(lower))
                return higher;
            else
                return cache(new FloatInterval(lower.low.compareTo(higher.low) > 0 ? higher.low : lower.low,
                        lower.high.compareTo(higher.high) < 0 ? higher.high : lower.high));
        }
    }

    /**
     * Yields {@code true} if this interval includes the given one.
     *
     * @param other the other interval
     *
     * @return {@code true} if it is included, {@code false} otherwise
     */
    public boolean includes(
            FloatInterval other) {
        return low.compareTo(other.low) <= 0 && high.compareTo(other.high) >= 0;
    }

    /**
     * Yields {@code true} if this interval intersects with the given one.
     *
     * @param other the other interval
     *
     * @return {@code true} if those intersects, {@code false} otherwise
     */
    public boolean intersects(
            FloatInterval other) {
        return includes(other) || other.includes(this)
                || (high.compareTo(other.low) >= 0 && high.compareTo(other.high) <= 0)
                || (other.high.compareTo(low) >= 0 && other.high.compareTo(high) <= 0);
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
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FloatInterval other = (FloatInterval) obj;
        if (high == null) {
            if (other.high != null)
                return false;
        } else if (!high.equals(other.high))
            return false;
        if (low == null) {
            if (other.low != null)
                return false;
        } else if (!low.equals(other.low))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + low + ", " + high + "]";
    }

    @Override
    public Iterator<Long> iterator() {
        if (!low.isFinite() || !high.isFinite() || low.isNaN() || high.isNaN())
            throw new InfiniteIterationFloatIntervalException(this);
        try {
            return new IntIntervalIterator(low.toLong(), high.toLong());
        } catch (MathNumberConversionException e) {
            throw new InfiniteIterationFloatIntervalException(this);
        }
    }

    @Override
    public int compareTo(
            FloatInterval o) {
        int cmp;
        if ((cmp = low.compareTo(o.low)) != 0)
            return cmp;
        return high.compareTo(o.high);
    }
}
