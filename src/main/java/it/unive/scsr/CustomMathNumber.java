package it.unive.scsr;

import it.unive.lisa.util.collections.CollectionUtilities;

import it.unive.scsr.CustomMathNumberConversionException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CustomMathNumber  implements Comparable<CustomMathNumber> {
    public static final CustomMathNumber PLUS_INFINITY = new CustomMathNumber((byte)1);
    public static final CustomMathNumber MINUS_INFINITY = new CustomMathNumber((byte)-1);
    public static final CustomMathNumber ZERO = new CustomMathNumber(0L);
    public static final CustomMathNumber ONE = new CustomMathNumber(1L);
    public static final CustomMathNumber MINUS_ONE = new CustomMathNumber(-1L);
    public static final CustomMathNumber NaN = new CustomMathNumber((byte)3);
    private final BigDecimal number;
    private final byte sign;

    public CustomMathNumber(long number) {
        this.number = BigDecimal.valueOf(number);
        this.sign = (byte)(number > 0L ? 1 : (number == 0L ? 0 : -1));
    }

    public CustomMathNumber(double number) {
        this.number = BigDecimal.valueOf(number);
        this.sign = (byte)(number > (double)0.0F ? 1 : (number == (double)0.0F ? 0 : -1));
    }

    public CustomMathNumber(float number) {
        this.number = BigDecimal.valueOf(number).setScale(5, RoundingMode.HALF_EVEN);
        this.sign = (byte)(number > (float)0.0F ? 1 : (number == (float)0.0F ? 0 : -1));
    }

    public CustomMathNumber(BigDecimal number) {
        this.number = number.setScale(5, RoundingMode.HALF_EVEN);
        this.sign = (byte)(number.signum() > 0 ? 1 : (number.signum() == 0 ? 0 : -1));
    }

    private CustomMathNumber(byte sign) {
        this.number = null;
        this.sign = sign;
    }

    public boolean isMinusInfinity() {
        return this.number == null && this.isNegative();
    }

    public boolean isPlusInfinity() {
        return this.number == null && this.isPositive();
    }

    public boolean isInfinite() {
        return this.isPlusInfinity() || this.isMinusInfinity();
    }

    public boolean isFinite() {
        return !this.isInfinite();
    }

    public boolean is(int n) {
        return this.number != null && this.number.equals(new BigDecimal(n));
    }

    public boolean isZero() {
        return this.sign == 0;
    }

    public boolean isNegative() {
        return this.sign == -1;
    }

    public boolean isPositive() {
        return this.sign == 1;
    }

    public boolean isNaN() {
        return this.number == null && this.sign == 3;
    }

    private static CustomMathNumber cached(CustomMathNumber i) {
        if (i.isZero()) {
            return ZERO;
        } else if (i.is(1)) {
            return ONE;
        } else {
            return i.is(-1) ? MINUS_ONE : i;
        }
    }

    public CustomMathNumber add(CustomMathNumber other) {
        if (!this.isNaN() && !other.isNaN()) {
            if (this.isInfinite()) {
                if (other.isFinite()) {
                    return this;
                } else {
                    return this.equals(other) ? this : NaN;
                }
            } else {
                return other.isInfinite() ? other : cached(new CustomMathNumber(this.number.add(other.number)));
            }
        } else {
            return NaN;
        }
    }

    public CustomMathNumber subtract(CustomMathNumber other) {
        if (!this.isNaN() && !other.isNaN()) {
            if (this.isInfinite()) {
                if (other.isFinite()) {
                    return this;
                } else {
                    return this.equals(other) ? NaN : this;
                }
            } else {
                return other.isInfinite() ? other.multiply(MINUS_ONE) : cached(new CustomMathNumber(this.number.subtract(other.number)));
            }
        } else {
            return NaN;
        }
    }

    public CustomMathNumber multiply(CustomMathNumber other) {
        if (!this.isNaN() && !other.isNaN()) {
            if (this.isInfinite()) {
                if (other.isZero()) {
                    return NaN;
                } else {
                    return this.sign == other.sign ? PLUS_INFINITY : MINUS_INFINITY;
                }
            } else if (other.isInfinite()) {
                if (this.isZero()) {
                    return NaN;
                } else {
                    return this.sign == other.sign ? PLUS_INFINITY : MINUS_INFINITY;
                }
            } else {
                return !this.isZero() && !other.isZero() ? cached(new CustomMathNumber(this.number.multiply(other.number))) : ZERO;
            }
        } else {
            return NaN;
        }
    }

    public CustomMathNumber divide(CustomMathNumber other) {
        if (!this.isNaN() && !other.isNaN() && !other.isZero() && (!this.isInfinite() || !other.isInfinite())) {
            if (this.isZero()) {
                return ZERO;
            } else if (!other.isPlusInfinity() && !other.isMinusInfinity()) {
                if (!this.isPlusInfinity() && !this.isMinusInfinity()) {
                    return cached(new CustomMathNumber(this.number.divide(other.number, 100, RoundingMode.HALF_UP).stripTrailingZeros()));
                } else {
                    return this.isPositive() == other.isPositive() ? PLUS_INFINITY : MINUS_INFINITY;
                }
            } else {
                return ZERO;
            }
        } else {
            return NaN;
        }
    }

    public int compareTo(CustomMathNumber other) {
        if (this.equals(other)) {
            return 0;
        } else if (this.isNaN() && !other.isNaN()) {
            return -1;
        } else if (!this.isNaN() && other.isNaN()) {
            return 1;
        } else if (this.isNaN()) {
            return 0;
        } else {
            int s = Byte.compare(this.sign, other.sign);
            if (s != 0) {
                return s;
            } else if (!this.isMinusInfinity() && !other.isPlusInfinity()) {
                return !this.isPlusInfinity() && !other.isMinusInfinity() ? CollectionUtilities.nullSafeCompare(true, this.number, other.number, BigDecimal::compareTo) : 1;
            } else {
                return -1;
            }
        }
    }

    public CustomMathNumber min(CustomMathNumber other) {
        if (!this.isNaN() && !other.isNaN()) {
            if (!this.isMinusInfinity() && !other.isPlusInfinity()) {
                return !other.isMinusInfinity() && !this.isPlusInfinity() ? cached(new CustomMathNumber(this.number.min(other.number))) : other;
            } else {
                return this;
            }
        } else {
            return NaN;
        }
    }

    public CustomMathNumber max(CustomMathNumber other) {
        if (!this.isNaN() && !other.isNaN()) {
            if (!other.isMinusInfinity() && !this.isPlusInfinity()) {
                return !this.isMinusInfinity() && !other.isPlusInfinity() ? cached(new CustomMathNumber(this.number.max(other.number))) : other;
            } else {
                return this;
            }
        } else {
            return NaN;
        }
    }

    public boolean leq(CustomMathNumber other) {
        return this.max(other).equals(other);
    }

    public boolean gt(CustomMathNumber other) {
        return this.geq(other) && !this.equals(other);
    }

    public boolean lt(CustomMathNumber other) {
        return this.leq(other) && !this.equals(other);
    }

    public boolean geq(CustomMathNumber other) {
        return this.max(other).equals(this);
    }

    public CustomMathNumber abs() {
        if (this.isNaN()) {
            return NaN;
        } else if (this.isPlusInfinity()) {
            return this;
        } else {
            return this.isMinusInfinity() ? PLUS_INFINITY : cached(new CustomMathNumber(this.number.abs()));
        }
    }

    public CustomMathNumber roundUp() {
        return !this.isInfinite() && !this.isNaN() ? cached(new CustomMathNumber(this.number.setScale(0, RoundingMode.CEILING))) : this;
    }

    public CustomMathNumber roundDown() {
        return !this.isInfinite() && !this.isNaN() ? cached(new CustomMathNumber(this.number.setScale(0, RoundingMode.FLOOR))) : this;
    }

    public int toInt() throws CustomMathNumberConversionException {
        if (!this.isNaN() && !this.isInfinite()) {
            return this.number.intValue();
        } else {
            throw new CustomMathNumberConversionException(this);
        }
    }

    public double toDouble() throws CustomMathNumberConversionException {
        if (!this.isNaN() && !this.isInfinite()) {
            return this.number.doubleValue();
        } else {
            throw new CustomMathNumberConversionException(this);
        }
    }

    public byte toByte() throws CustomMathNumberConversionException {
        if (!this.isNaN() && !this.isInfinite()) {
            return this.number.byteValue();
        } else {
            throw new CustomMathNumberConversionException(this);
        }
    }

    public short toShort() throws CustomMathNumberConversionException {
        if (!this.isNaN() && !this.isInfinite()) {
            return this.number.shortValue();
        } else {
            throw new CustomMathNumberConversionException(this);
        }
    }

    public float toFloat() throws CustomMathNumberConversionException {
        if (!this.isNaN() && !this.isInfinite()) {
            return this.number.floatValue();
        } else {
            throw new CustomMathNumberConversionException(this);
        }
    }

    public long toLong() throws CustomMathNumberConversionException {
        if (!this.isNaN() && !this.isInfinite()) {
            return this.number.longValue();
        } else {
            throw new CustomMathNumberConversionException(this);
        }
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.number == null ? 0 : this.number.hashCode());
        result = 31 * result + this.sign;
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
            CustomMathNumber other = (CustomMathNumber)obj;
            if (this.number == null) {
                if (other.number != null) {
                    return false;
                }
            } else if (!this.number.equals(other.number)) {
                return false;
            }

            return this.sign == other.sign;
        }
    }

    public String toString() {
        return this.isNaN() ? "NaN" : (this.isMinusInfinity() ? "-Inf" : (this.isPlusInfinity() ? "+Inf" : this.number.toString()));
    }

    public BigDecimal getNumber() {
        if (this.isNaN()) {
            throw new IllegalStateException();
        } else if (this.isPlusInfinity()) {
            throw new IllegalStateException();
        } else if (this.isMinusInfinity()) {
            throw new IllegalStateException();
        } else {
            return this.number;
        }
    }
}