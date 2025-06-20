package it.unive.scsr;

import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;

import java.math.RoundingMode;
import java.util.Objects;

public class FloatInterval implements Comparable<FloatInterval>{

    public static final FloatInterval INFINITY = new FloatInterval();
    public static final FloatInterval ZERO = new FloatInterval(0.0, 0.0);
    public static final FloatInterval ONE = new FloatInterval(1.0, 1.0);
    public static final FloatInterval MINUS_ONE = new FloatInterval(-1.0, -1.0);

    private final double low;
    private final double high;


    public FloatInterval(double low, double high){
        if (!Double.isNaN(low) && !Double.isNaN(high)) {
            if(compareMath(low, high) /*Double.compare(low, high) */<= 0){
                this.low = low;
                this.high = high;
            }else{
                this.low = high;
                this.high = low;
            }
        }else{
            this.low = Double.NaN;
            this.high = Double.NaN;
        }

    }

    public FloatInterval(){
        this(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }


    public double getLow() {
        return low;
    }

    public double getHigh() {
        return high;
    }

    public boolean lowIsMinusInfinity() {
        return this.low == Double.NEGATIVE_INFINITY;
    }

    public boolean highIsPlusInfinity() {
        return this.high == Double.POSITIVE_INFINITY;
    }


    public boolean isInfinity() {
        return Double.isInfinite(low) && Double.isInfinite(high);
    }

    public boolean isInfinite() {
        return this == INFINITY || this.highIsPlusInfinity() || this.lowIsMinusInfinity();
    }

    public boolean isFinite() {
        return !this.isInfinite();
    }

    public boolean includes(FloatInterval other) {
        return compareMath(this.low, other.low) <= 0 && compareMath(this.high, other.high) >= 0;
        //return Double.compare(this.getLow(), other.getLow()) <= 0 && Double.compare(this.getHigh(), other.getHigh()) >= 0;
        //this.low <= other.low && this.high >= other.high;
    }

    public boolean intersects(FloatInterval other) {
        /*return this.includes(other) || other.includes(this) ||
                Double.compare(this.getHigh(), getLow()) >= 0 &&
                        Double.compare(this.getHigh(), other.getHigh()) <= 0 ||
                Double.compare(other.getHigh(), this.getLow()) >=0 &&
                        Double.compare(other.getHigh(), this.getHigh()) <= 0;*/
        return this.includes(other) || other.includes(this) ||
                compareMath(this.high, other.low) >= 0 &&
                        compareMath(this.high, other.high) <= 0 ||
                compareMath(other.high, this.low) >=0 &&
                        compareMath(other.high, this.high) <= 0;
    }

    @Override
    public int compareTo(FloatInterval o) {
        /*if (this.low != o.low)
            return Double.compare(this.low, o.low);
        return Double.compare(this.high, o.high);*/
        /*int cmp = Double.compare(this.low, o.low);
        return (cmp != 0) ? cmp : Double.compare(this.high, o.high);*/
        int cmp = compareMath(this.low, o.low);
        return (cmp != 0) ? cmp : compareMath(this.high, o.high);
    }



    @Override
    public int hashCode() {
        return Objects.hash(low, high);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof FloatInterval))
            return false;
        FloatInterval other = (FloatInterval) obj;
        /*return Double.compare(this.low, other.low) == 0 &&
                Double.compare(this.high, other.high) == 0;*/
        return compareMath(this.low, other.low) == 0 &&
                compareMath(this.high, other.high) == 0;
    }

    @Override
    public String toString() {
        return "[" + low + ", " + high + "]";
    }

    public double min(double num){
        if(this.low <= num){
            return this.low;
        }
        return num;
    }

    public double max(double num){
        if(this.high >= num){
            return this.high;
        }
        return num;
    }
    public boolean isSingleton() {
        return this.isFinite() && equalsMath(this.low, this.high);
    }

    public static double subtract(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) {
            if (Double.isInfinite(a)) {
                if (Double.isFinite(b)) {
                    return a;
                } else {
                    return a == b ? Double.NaN : a;
                }
            } else {
                if (Double.isInfinite(b)) {
                    return -b;
                } else {
                    return a - b;
                }
            }
        } else {
            return Double.NaN;
        }
    }

    public static double add(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) {
            if (Double.isInfinite(a)) {
                if (Double.isFinite(b)) {
                    return a;
                } else {
                    return a == b ? a : Double.NaN;
                }
            } else {
                return Double.isInfinite(b) ? b : a + b;
            }
        } else {
            return Double.NaN;
        }
    }

    public static double multiply(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b)) {
            if (Double.isInfinite(a)) {
                if (b == 0.0) {
                    return Double.NaN;
                } else {
                    return Math.copySign(Double.POSITIVE_INFINITY, a * b);
                }
            } else if (Double.isInfinite(b)) {
                if (a == 0.0) {
                    return Double.NaN;
                } else {
                    return Math.copySign(Double.POSITIVE_INFINITY, a * b);
                }
            } else {
                return a * b;
            }
        } else {
            return Double.NaN;
        }
    }


    public static double divide(double a, double b) {
        if (!Double.isNaN(a) && !Double.isNaN(b) && b != 0.0 && !(Double.isInfinite(a) && Double.isInfinite(b))) {
            if (a == 0.0) {
                return 0.0;
            } else if (!Double.isInfinite(b)) {
                if (!Double.isInfinite(a)) {
                    return a / b;
                } else {
                    return Math.copySign(Double.POSITIVE_INFINITY, a * b);
                }
            } else {
                return 0.0;
            }
        } else {
            return Double.NaN;
        }
    }

    public static int compareMath(double a, double b) {
        // Cases when is NaN
        boolean aNaN = Double.isNaN(a);
        boolean bNaN = Double.isNaN(b);

        if (aNaN && bNaN)
            return 0;
        if (aNaN)
            return -1;
        if (bNaN)
            return 1;

        // Compare of sign
        int signCompare = Double.compare(Math.signum(a), Math.signum(b));
        if (signCompare != 0)
            return signCompare;

        // Cases when is infinity
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            if (a == b)
                return 0;
            return a < b ? -1 : 1;
        }

        // Finite values: numerical compare
        return Double.compare(a, b);
    }

    public static boolean equalsMath(double a, double b) {
        // Entrambi NaN → considerati uguali (come nel tuo MathNumber.equals)
        if (Double.isNaN(a) && Double.isNaN(b))
            return true;

        // Uno solo è NaN → diversi
        if (Double.isNaN(a) || Double.isNaN(b))
            return false;

        // +0.0 e -0.0: vuoi considerarli diversi? Se sì:
        if (Double.doubleToRawLongBits(a) != Double.doubleToRawLongBits(b))
            return false;

        // Altrimenti confronto diretto (copre infiniti, numeri regolari, ecc.)
        return a == b;
    }


}
