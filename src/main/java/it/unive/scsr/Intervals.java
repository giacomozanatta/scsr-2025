package it.unive.scsr;

import java.util.Objects;
import java.util.Optional;

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
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.intervals.BinaryFunctions;
import it.unive.scsr.intervals.NumericInterval;
import it.unive.scsr.intervals.numbers.*;

public class Intervals implements BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

    /**
     * The interval represented by this domain element.
     */
    public final NumericInterval interval;

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
    public Intervals(SigNum lower, SigNum upper) {
        this(new NumericInterval(lower, upper));
    }

    @Override
    public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle) {
        // If the specified argument is bottom or top, the input is returned as output without performing any
        // calculations.
        if (arg.isTop()) return arg;

        if (operator instanceof NumericNegation) {
            // Given a MathNumber element, the negation is calculated taking into account the possibility of handling an
            // infinite element.
            return interval
                    .negate()
                    .map(Intervals::new)
                    .orElse(BOTTOM);
        }

        return top();
    }

    @Override
    public Intervals glbAux(Intervals other) {
        return this
                .interval
                .intersection(other.interval)
                .map(Intervals::new)
                .orElse(BOTTOM);
    }

    @Override
    public Intervals lubAux(Intervals other) {
        return this
                .interval
                .union(other.interval)
                .map(Intervals::new)
                .orElse(BOTTOM);
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

    @Override
    public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) {
        // Cover all possible primitive numbers that can be deduced by analyzing the CFG.
        if (!(constant.getValue() instanceof Number number)) return top();

        // If there is a number that cannot be enclosed in a IntervalNumber, then top is returned.
        return IntervalNumber
                .ofPrimitive(number)
                .map(NumericInterval::new)
                .map(Intervals::new)
                .orElse(top());
    }

    @Override
    public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
                                          SemanticOracle oracle) {
        return Optional
                .ofNullable(BinaryFunctions.INSTANCE.findBy(operator))
                .map(f -> f.apply(left, right))
                .orElse(top());
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
        IntervalNumber newLower, newUpper;
        if (other.interval.high.compareTo(interval.high) > 0) {
            // High value is increasing.
            newUpper = PlusInfinity.INSTANCE;
        } else {
            newUpper = interval.high;
        }

        if (other.interval.low.compareTo(interval.low) < 0) {
            // Low value is decreasing.
            newLower = MinusInfinity.INSTANCE;
        } else {
            newLower = interval.low;
        }

        return new Intervals((SigNum) newLower, (SigNum) newUpper);
    }

    @Override
    public Intervals narrowingAux(
            Intervals other) {
        SigNum newHigh = (SigNum) (interval.high instanceof PlusInfinity ? other.interval.high : interval.high);
        SigNum newLow = (SigNum) (interval.low instanceof MinusInfinity ? other.interval.low : interval.low);
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
