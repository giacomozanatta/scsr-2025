package it.unive.scsr;

import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals
        // instances of this class are lattice elements such that:
        // - their state (fields) hold the information contained into a single
        // variable
        // - they provide logic for the evaluation of expressions
        implements
        BaseNonRelationalValueDomain<
                // java requires this type parameter to have this class
                // as type in fields/methods
                Intervals>,
        Comparable<Intervals> {

    /**
     * The interval represented by this domain element.
     */
    public final IntInterval interval;

    /**
     * The abstract zero ({@code [0, 0]}) element.
     */
    public static final Intervals ZERO = new Intervals(IntInterval.ZERO);

    /**
     * The abstract top ({@code [-Inf, +Inf]}) element.
     */
    public static final Intervals TOP = new Intervals(IntInterval.INFINITY);

    /**
     * The abstract bottom element.
     */
    public static final Intervals BOTTOM = new Intervals(null);

    /**
     * Builds the interval.
     * 
     * @param interval the underlying {@link IntInterval}
     */
    public Intervals(
            IntInterval interval) {
        this.interval = interval;
    }

    /**
     * Builds the interval.
     * 
     * @param low  the lower bound
     * @param high the higher bound
     */
    public Intervals(
            MathNumber low,
            MathNumber high) {
        this(new IntInterval(low, high));
    }

    /**
     * Builds the interval.
     * 
     * @param low  the lower bound
     * @param high the higher bound
     */
    public Intervals(
            int low,
            int high) {
        this(new IntInterval(low, high));
    }

    /**
     * Builds the top interval.
     */
    public Intervals() {
        this(IntInterval.INFINITY);
    }

    @Override
    public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (operator == NumericNegation.INSTANCE) {
            if (arg.isTop()) {
                return top();
            } else {
                return new Intervals(arg.interval.mul(IntInterval.MINUS_ONE));
            }
        } else if (operator instanceof NegatableOperator) {
            return new Intervals(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
        } else {
            return top();
        }
    }

    @Override
    public Intervals glbAux(Intervals other) throws SemanticException {
        IntInterval a = this.interval;
        IntInterval b = other.interval;

        MathNumber lA = a.getLow();
        MathNumber lB = b.getLow();

        MathNumber uA = a.getHigh();
        MathNumber uB = b.getHigh();

        if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
            return BOTTOM;

        MathNumber newLower = lA.max(lB);
        MathNumber newUpper = uA.min(uB);

        Intervals newInterval = new Intervals(newLower, newUpper);

        return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
    }

    @Override
    public Intervals lubAux(Intervals other) throws SemanticException {
        IntInterval a = this.interval;
        IntInterval b = other.interval;

        MathNumber lA = a.getLow();
        MathNumber lB = b.getLow();

        MathNumber uA = a.getHigh();
        MathNumber uB = b.getHigh();

        MathNumber newLower = lA.min(lB);
        MathNumber newUpper = uA.max(uB);

        if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
            return BOTTOM;

        Intervals newInterval = new Intervals(newLower, newUpper);
        return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
    }

    @Override
    public boolean lessOrEqualAux(Intervals other) throws SemanticException {
        return other.interval.includes(this.interval);
    }

    @Override
    public Intervals top() {
        // the top element of the lattice is [-inf, +inf]
        return TOP;
    }

    @Override
    public boolean isTop() {
        return interval != null && interval.isInfinity();
    }

    @Override
    public Intervals bottom() {
        // the bottom element of the lattice is an element with a null interval
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return interval == null;
    }

    @Override
    public StructuredRepresentation representation() {
        if (this.isBottom())
            return Lattice.bottomRepresentation();

        return new StringRepresentation(this.interval.toString());
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
    public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            Integer i = (Integer) constant.getValue();
            Intervals singletonInterval = new Intervals(i, i);
            return singletonInterval;
        }

        return top();
    }

    @Override
    public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
            SemanticOracle oracle) throws SemanticException {

        if (left.isBottom() || right.isBottom())
            return bottom();

        if (operator instanceof AdditionOperator) {

            MathNumber leftLow = left.interval.getLow();
            MathNumber rightLow = right.interval.getLow();

            MathNumber leftHigh = left.interval.getHigh();
            MathNumber rightHigh = right.interval.getHigh();

            MathNumber sumLow = leftLow.add(rightLow);
            MathNumber sumHigh = leftHigh.add(rightHigh);

            return new Intervals(sumLow, sumHigh);

        } else if (operator instanceof SubtractionOperator) {

            MathNumber leftLow = left.interval.getLow();
            MathNumber rightLow = right.interval.getLow();

            MathNumber leftHigh = left.interval.getHigh();
            MathNumber rightHigh = right.interval.getHigh();

            MathNumber subLow = leftLow.subtract(rightLow);
            MathNumber subHigh = leftHigh.subtract(rightHigh);

            return new Intervals(subLow, subHigh);

        } else if (operator instanceof MultiplicationOperator) {
            if (left.is(0) || right.is(0)) {
                return ZERO;
            } else {
                return new Intervals(left.interval.mul(right.interval));
            }
        } else if (operator instanceof DivisionOperator) {
            if (right.is(0)) {
                return bottom();
            } else if (left.is(0)) {
                return ZERO;
            } else if (left.isTop() || right.isTop()) {
                return top();
            } else {
                return new Intervals(left.interval.div(right.interval, false, false));
            }
        } else if (operator instanceof ModuloOperator) {
            if (right.is(0)) {
                return bottom();
            } else if (left.is(0)) {
                return ZERO;
            } else if (left.isTop() || right.isTop()) {
                return top();
            } else {
                if (right.interval.getHigh().compareTo(MathNumber.ZERO) < 0) {
                    return new Intervals(right.interval.getLow().add(MathNumber.ONE), MathNumber.ZERO);
                } else if (right.interval.getLow().compareTo(MathNumber.ZERO) > 0) {
                    return new Intervals(MathNumber.ZERO, right.interval.getHigh().subtract(MathNumber.ONE));
                } else {
                    return new Intervals(right.interval.getLow().add(MathNumber.ONE),
                            right.interval.getHigh().subtract(MathNumber.ONE));
                }
            }
        } else if (operator instanceof RemainderOperator) {
            if (right.is(0)) {
                return bottom();
            } else if (left.is(0)) {
                return ZERO;
            } else if (left.isTop() || right.isTop()) {
                return top();
            } else {
                MathNumber mathNumber;
                if (right.interval.getHigh().compareTo(MathNumber.ZERO) < 0) {
                    mathNumber = right.interval.getLow().multiply(MathNumber.MINUS_ONE);
                } else if (right.interval.getLow().compareTo(MathNumber.ZERO) > 0) {
                    mathNumber = right.interval.getHigh();
                } else {
                    mathNumber = right.interval.getLow().abs().max(right.interval.getHigh().abs());
                }

                if (left.interval.getHigh().compareTo(MathNumber.ZERO) < 0) {
                    return new Intervals(mathNumber.multiply(MathNumber.MINUS_ONE).add(MathNumber.ONE),
                            MathNumber.ZERO);
                } else if (left.interval.getLow().compareTo(MathNumber.ZERO) > 0) {
                    return new Intervals(MathNumber.ZERO, mathNumber.subtract(MathNumber.ONE));
                } else {
                    return new Intervals(mathNumber.multiply(MathNumber.MINUS_ONE).add(MathNumber.ONE),
                            mathNumber.subtract(MathNumber.ONE));
                }
            }
        }
        return top();
    }

    /**
     * Tests whether this interval instance corresponds (i.e., concretizes)
     * exactly to the given integer. The tests is performed through
     * {@link IntInterval#is(int)}.
     * 
     * @param n the integer value
     * 
     * @return {@code true} if that condition holds
     */
    public boolean is(int n) {
        return !isBottom() && interval.is(n);
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

    // logic for widening below

    @Override
    public Intervals wideningAux(
            Intervals other)
            throws SemanticException {
        MathNumber newLow, newHigh;
        if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
            // high value is increasing
            newHigh = MathNumber.PLUS_INFINITY;
        else
            newHigh = interval.getHigh();

        if (other.interval.getLow().compareTo(interval.getLow()) < 0)
            // low value is decreasing
            newLow = MathNumber.MINUS_INFINITY;
        else
            newLow = interval.getLow();

        return newLow.isMinusInfinity() && newHigh.isPlusInfinity() ? top() : new Intervals(newLow, newHigh);
    }

    // logic for narrowing below

    @Override
    public Intervals narrowingAux(
            Intervals other)
            throws SemanticException {
        MathNumber newLow, newHigh;
        newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
        newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
        return new Intervals(newLow, newHigh);
    }

    @Override
    public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
            BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
            SemanticOracle oracle) throws SemanticException {
        Identifier identifier;
        Intervals eval;
        boolean rightExpression;
        if (left instanceof Identifier) {
            eval = eval(right, environment, src, oracle);
            identifier = (Identifier) left;
            rightExpression = true;
        } else if (right instanceof Identifier) {
            eval = eval(left, environment, src, oracle);
            identifier = (Identifier) right;
            rightExpression = false;
        } else
            return environment;

        Intervals start = environment.getState(identifier);
        if (eval.isBottom() || start.isBottom())
            return environment.bottom();

        boolean lowIsMinusInfinity = eval.interval.lowIsMinusInfinity();
        Intervals lowInfinity = new Intervals(eval.interval.getLow(), MathNumber.PLUS_INFINITY);
        Intervals lowPlusOneToInfinity = new Intervals(eval.interval.getLow().add(MathNumber.ONE),
                MathNumber.PLUS_INFINITY);
        Intervals highInfinity = new Intervals(MathNumber.MINUS_INFINITY, eval.interval.getHigh());
        Intervals upperBoundMinusOne = new Intervals(MathNumber.MINUS_INFINITY,
                eval.interval.getHigh().subtract(MathNumber.ONE));
        Intervals update = null;
        if (operator == ComparisonEq.INSTANCE)
            update = eval;
        else if (operator == ComparisonGe.INSTANCE)
            if (rightExpression)
                update = lowIsMinusInfinity ? null : start.glb(lowInfinity);
            else
                update = start.glb(highInfinity);
        else if (operator == ComparisonGt.INSTANCE)
            if (rightExpression)
                update = lowIsMinusInfinity ? null : start.glb(lowPlusOneToInfinity);
            else
                update = lowIsMinusInfinity ? eval : start.glb(upperBoundMinusOne);
        else if (operator == ComparisonLe.INSTANCE)
            if (rightExpression)
                update = start.glb(highInfinity);
            else
                update = lowIsMinusInfinity ? null : start.glb(lowInfinity);
        else if (operator == ComparisonLt.INSTANCE)
            if (rightExpression)
                update = lowIsMinusInfinity ? eval : start.glb(upperBoundMinusOne);
            else
                update = lowIsMinusInfinity ? null : start.glb(lowPlusOneToInfinity);

        if (update == null)
            return environment;
        else if (update.isBottom())
            return environment.bottom();
        else
            return environment.putState(identifier, update);
    }

}