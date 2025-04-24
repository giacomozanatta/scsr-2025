package it.unive.scsr;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.intervals.BinaryFunctions;
import it.unive.scsr.intervals.NumericInterval;

public class Intervals implements
        BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

    /**
     * The interval represented by this domain element.
     */
    public final NumericInterval interval;

    /**
     * The abstract zero ({@code [0, 0]}) element.
     */
    public static final Intervals ZERO = new Intervals(NumericInterval.ZERO);

    /**
     * The abstract top ({@code [-Inf, +Inf]}) element.
     */
    public static final Intervals TOP = new Intervals(NumericInterval.INFINITY);

    /**
     * The abstract bottom element.
     */
    public static final Intervals BOTTOM = new Intervals(null);

    /**
     * Builds the interval.
     *
     * @param interval the underlying {@link IntInterval}
     */
    public Intervals(NumericInterval interval) {
        this.interval = interval;
    }

    /**
     * Builds the interval.
     *
     * @param lower the lower bound
     * @param upper the higher bound
     */
    public Intervals(MathNumber lower, MathNumber upper) {
        this(new NumericInterval(lower, upper));
    }

    /**
     * Builds the interval.
     *
     * @param low  the lower bound
     * @param high the higher bound
     */
    public Intervals(int low, int high) {
        this(new NumericInterval(
                new NumericInterval.IntervalNumber.Long(low),
                new NumericInterval.IntervalNumber.Long(high)));
    }

    /**
     * Builds the top interval.
     */
    public Intervals() {
        this(NumericInterval.INFINITY);
    }

    @Override
    public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle) {
        // If the specified argument is bottom or top, the input is returned as output without performing any
        // calculations.
        if (arg.isTop()) return arg;

        // If one of the elements is NaN, the lower element is returned since the computation cannot continue.
        if (arg.interval.low.isNaN() || arg.interval.high.isNaN()) return BOTTOM;

        if (operator instanceof NumericNegation) {
            // Given a MathNumber element, the negation is calculated taking into account the possibility of handling an
            // infinite element.
            Function<MathNumber, MathNumber> f = n ->
                    n.isInfinite() ?
                            (n.isMinusInfinity() ? MathNumber.PLUS_INFINITY : MathNumber.MINUS_INFINITY) :
                            new MathNumber(n.getNumber().negate());

            // The minimum and maximum elements are reversed.
            return new Intervals(f.apply(arg.interval.high), f.apply(arg.interval.low));
        }

        return TOP;
    }

    @Override
    public Intervals glbAux(Intervals other) {

        NumericInterval a = this.interval;
        NumericInterval b = other.interval;

        MathNumber lA = a.low;
        MathNumber lB = b.low;

        MathNumber uA = a.high;
        MathNumber uB = b.high;

        if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
            return BOTTOM;

        MathNumber newLower = lA.max(lB);
        MathNumber newUpper = uA.min(uB);

        Intervals newInterval = new Intervals(newLower, newUpper);

        return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
    }

    @Override
    public Intervals lubAux(Intervals other) {

        NumericInterval a = this.interval;
        NumericInterval b = other.interval;

        MathNumber lA = a.low;
        MathNumber lB = b.low;

        MathNumber uA = a.high;
        MathNumber uB = b.high;

        MathNumber newLower = lA.min(lB);
        MathNumber newUpper = uA.max(uB);

        if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
            return BOTTOM;

        Intervals newInterval = new Intervals(newLower, newUpper);
        return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() :
                newInterval;
    }

    @Override
    public boolean lessOrEqualAux(Intervals other) {
        return other
                .interval
                .includes(this.interval);
    }


    @Override
    public Intervals top() {
        // The top element of the lattice is [-inf, +inf].
        return TOP;
    }

    @Override
    public boolean isTop() {
        return interval != null && interval.isInfinity();
    }

    @Override
    public Intervals bottom() {
        // The bottom element of the lattice is an element with a null interval.
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return interval == null;
    }

    @Override
    public StructuredRepresentation representation() {
        if (this.isBottom()) return Lattice.bottomRepresentation();
        return new StringRepresentation("[" + this.interval.low + "," + this.interval.high + "]");
    }

    @Override
    public int compareTo(Intervals o) {
        if (isBottom())
            return o.isBottom() ? 0 : -1;
        if (isTop())
            return o.isTop() ? 0 : 1;

        if (o.isBottom())
            return 1;

        if (isTop())
            return -1;

        return interval.compareTo(o.interval);
    }

    // logic for evaluating expressions below

    @Override
    public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) {
        if (constant.getValue() instanceof Integer i) return new Intervals(i, i);
        return top();
    }

    @Override
    public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
                                          SemanticOracle oracle) {
        return Optional
                .ofNullable(BinaryFunctions.INSTANCE.findBy(operator))
                .map(f -> f.apply(left, right))
                .orElse(TOP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Intervals other = (Intervals) obj;
        return Objects.equals(interval, other.interval);
    }

    @Override
    public Intervals wideningAux(Intervals other) {
        MathNumber newLower, newUpper;
        if (other.interval.high.compareTo(interval.high) > 0) {
            // High value is increasing.
            newUpper = MathNumber.PLUS_INFINITY;
        } else {
            newUpper = interval.high;
        }

        if (other.interval.low.compareTo(interval.low) < 0) {
            // Low value is decreasing.
            newLower = MathNumber.MINUS_INFINITY;
        } else {
            newLower = interval.low;
        }

        return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : new Intervals(newLower, newUpper);
    }

    @Override
    public Intervals narrowingAux(
            Intervals other) {
        MathNumber newHigh = interval.high.isInfinite() ? other.interval.high : interval.high;
        MathNumber newLow = interval.low.isInfinite() ? other.interval.low : interval.low;
        return new Intervals(newLow, newHigh);
    }


    @Override
    public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
                                                              BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
                                                              SemanticOracle oracle) throws SemanticException {

        // Any assumptions should be implemented here!
        return BaseNonRelationalValueDomain
                .super
                .assumeBinaryExpression(environment, operator, left, right, src, dest, oracle);
    }
}
