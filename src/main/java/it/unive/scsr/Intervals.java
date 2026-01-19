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
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals
        implements BaseNonRelationalValueDomain<Intervals>, Comparable<Intervals> {

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
    public Intervals(IntInterval interval) {
        this.interval = interval;
    }

    /**
     * Builds the interval.
     *
     * @param lower the lower bound
     * @param upper the higher bound
     */
    public Intervals(MathNumber lower, MathNumber upper) {
        this(new IntInterval(lower, upper));
    }

    /**
     * Builds the interval.
     *
     * @param low  the lower bound
     * @param high the higher bound
     */
    public Intervals(int low, int high) {
        this(new IntInterval(low, high));
    }

    /**
     * Builds the top interval.
     */
    public Intervals() {
        this(IntInterval.INFINITY);
    }

    @Override
    public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp,
                                         SemanticOracle oracle)
            throws SemanticException {

        if (arg.isBottom())
            return bottom();

        // Negation: -[a,b] = [-b, -a]
        if (operator instanceof NumericNegation) {
            MathNumber low = arg.interval.getLow();
            MathNumber high = arg.interval.getHigh();

            // Negate by multiplying by -1 and swap bounds
            MathNumber newLow = high.multiply(new MathNumber(-1));
            MathNumber newHigh = low.multiply(new MathNumber(-1));

            return new Intervals(newLow, newHigh);
        }

        return top();
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

        // Check if intersection is empty
        if (newLower.compareTo(newUpper) > 0)
            return BOTTOM;

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
        return TOP;
    }

    @Override
    public boolean isTop() {
        return interval != null && interval.isInfinity();
    }

    @Override
    public Intervals bottom() {
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

        return new StringRepresentation("[" + this.interval.getLow() + "," + this.interval.getHigh() + "]");
    }

    @Override
    public int compareTo(Intervals o) {
        if (isBottom())
            return o.isBottom() ? 0 : -1;
        if (isTop())
            return o.isTop() ? 0 : 1;

        if (o.isBottom())
            return 1;

        if (o.isTop())
            return -1;

        return interval.compareTo(o.interval);
    }

    @Override
    public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        Object value = constant.getValue();

        // Handle integer constants
        if (value instanceof Integer) {
            Integer i = (Integer) value;
            return new Intervals(i, i);
        }

        // Handle long constants
        if (value instanceof Long) {
            Long l = (Long) value;
            return new Intervals(new MathNumber(l), new MathNumber(l));
        }

        // Handle float constants
        if (value instanceof Float) {
            Float f = (Float) value;
            return new Intervals(new MathNumber(f.doubleValue()), new MathNumber(f.doubleValue()));
        }

        // Handle double constants
        if (value instanceof Double) {
            Double d = (Double) value;
            return new Intervals(new MathNumber(d), new MathNumber(d));
        }

        // Handle byte constants
        if (value instanceof Byte) {
            Byte b = (Byte) value;
            return new Intervals(b.intValue(), b.intValue());
        }

        // Handle short constants
        if (value instanceof Short) {
            Short s = (Short) value;
            return new Intervals(s.intValue(), s.intValue());
        }

        return top();
    }

    @Override
    public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
                                          SemanticOracle oracle) throws SemanticException {

        if (left.isBottom() || right.isBottom())
            return bottom();

        IntInterval a = left.interval;
        IntInterval b = right.interval;

        MathNumber lA = a.getLow();
        MathNumber lB = b.getLow();

        MathNumber uA = a.getHigh();
        MathNumber uB = b.getHigh();

        // Addition: [a,b] + [c,d] = [a+c, b+d]
        if (operator instanceof AdditionOperator) {
            return new Intervals(lA.add(lB), uA.add(uB));
        }

        // Subtraction: [a,b] - [c,d] = [a-d, b-c]
        else if (operator instanceof SubtractionOperator) {
            return new Intervals(lA.subtract(uB), uA.subtract(lB));
        }

        // Multiplication: [a,b] * [c,d]
        // Need to consider all four corner products and take min/max
        else if (operator instanceof MultiplicationOperator) {
            MathNumber p1 = lA.multiply(lB);
            MathNumber p2 = lA.multiply(uB);
            MathNumber p3 = uA.multiply(lB);
            MathNumber p4 = uA.multiply(uB);

            MathNumber newLow = p1.min(p2).min(p3).min(p4);
            MathNumber newHigh = p1.max(p2).max(p3).max(p4);

            return new Intervals(newLow, newHigh);
        }

        // Division: [a,b] / [c,d]
        // More complex due to division by zero and sign changes
        else if (operator instanceof DivisionOperator) {
            // Check if divisor interval contains zero
            if (lB.compareTo(MathNumber.ZERO) <= 0 && uB.compareTo(MathNumber.ZERO) >= 0) {
                // Division by interval containing zero - return top for safety
                return top();
            }

            // If divisor doesn't contain zero, compute all corner divisions
            MathNumber d1 = lA.divide(lB);
            MathNumber d2 = lA.divide(uB);
            MathNumber d3 = uA.divide(lB);
            MathNumber d4 = uA.divide(uB);

            MathNumber newLow = d1.min(d2).min(d3).min(d4);
            MathNumber newHigh = d1.max(d2).max(d3).max(d4);

            return new Intervals(newLow, newHigh);
        }

        // Modulo: [a,b] % [c,d]
        // Conservative approximation
        else if (operator instanceof ModuloOperator) {
            // If divisor is positive constant, we can be more precise
            if (lB.compareTo(MathNumber.ZERO) > 0 && lB.equals(uB)) {
                // Result is in range [0, divisor-1] for positive modulo
                return new Intervals(MathNumber.ZERO, uB.subtract(MathNumber.ONE));
            }
            // Conservative: result could be anywhere in the range
            MathNumber absMax = uB.abs().max(lB.abs());
            return new Intervals(absMax.multiply(new MathNumber(-1)), absMax);
        }

        return top();
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
    public Intervals wideningAux(Intervals other) throws SemanticException {
        MathNumber newLower, newUpper;

        if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
            // high value is increasing
            newUpper = MathNumber.PLUS_INFINITY;
        else
            newUpper = interval.getHigh();

        if (other.interval.getLow().compareTo(interval.getLow()) < 0)
            // low value is decreasing
            newLower = MathNumber.MINUS_INFINITY;
        else
            newLower = interval.getLow();

        return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : new Intervals(newLower, newUpper);
    }

    @Override
    public Intervals narrowingAux(Intervals other) throws SemanticException {
        MathNumber newLow, newHigh;
        newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
        newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
        return new Intervals(newLow, newHigh);
    }

    @Override
    public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
                                                              BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
                                                              SemanticOracle oracle) throws SemanticException {

        // IMPORTANT: Always return a non-null environment to avoid NullPointerException in Pentagons
        if (environment == null) {
            return environment;
        }

        // Implement assumptions for comparison operators
        // This refines the intervals based on conditional expressions

        // Get the identifiers involved (if any)
        Identifier leftId = null;
        Identifier rightId = null;
        Intervals rightInterval = null;

        if (left instanceof Identifier) {
            leftId = (Identifier) left;
        }

        if (right instanceof Identifier) {
            rightId = (Identifier) right;
        } else if (right instanceof Constant) {
            // Evaluate the constant to get its interval
            try {
                rightInterval = evalNonNullConstant((Constant) right, src, oracle);
            } catch (Exception e) {
                // If evaluation fails, return environment unchanged
                return environment;
            }
        }

        // Only handle cases where left is an identifier
        if (leftId == null) {
            return environment;
        }

        Intervals leftCurrentInterval = environment.getState(leftId);
        if (leftCurrentInterval == null || leftCurrentInterval.isBottom()) {
            return environment;
        }

        // Get right interval (either from identifier or constant)
        if (rightId != null) {
            rightInterval = environment.getState(rightId);
        }

        if (rightInterval == null || rightInterval.isBottom()) {
            return environment;
        }

        // Now apply assumptions based on the operator
        Intervals refinedInterval = null;

        try {
            // x == c: refine x to [c, c]
            if (operator instanceof ComparisonEq) {
                refinedInterval = leftCurrentInterval.glb(rightInterval);
            }
            // x != c: no refinement possible in general
            else if (operator instanceof ComparisonNe) {
                // Could remove single point, but complex for intervals
                return environment;
            }
            // x < c: refine x to [-inf, c-1]
            else if (operator instanceof ComparisonLt) {
                MathNumber upperBound = rightInterval.interval.getLow().subtract(MathNumber.ONE);
                Intervals constraint = new Intervals(MathNumber.MINUS_INFINITY, upperBound);
                refinedInterval = leftCurrentInterval.glb(constraint);
            }
            // x <= c: refine x to [-inf, c]
            else if (operator instanceof ComparisonLe) {
                MathNumber upperBound = rightInterval.interval.getHigh();
                Intervals constraint = new Intervals(MathNumber.MINUS_INFINITY, upperBound);
                refinedInterval = leftCurrentInterval.glb(constraint);
            }
            // x > c: refine x to [c+1, +inf]
            else if (operator instanceof ComparisonGt) {
                MathNumber lowerBound = rightInterval.interval.getHigh().add(MathNumber.ONE);
                Intervals constraint = new Intervals(lowerBound, MathNumber.PLUS_INFINITY);
                refinedInterval = leftCurrentInterval.glb(constraint);
            }
            // x >= c: refine x to [c, +inf]
            else if (operator instanceof ComparisonGe) {
                MathNumber lowerBound = rightInterval.interval.getLow();
                Intervals constraint = new Intervals(lowerBound, MathNumber.PLUS_INFINITY);
                refinedInterval = leftCurrentInterval.glb(constraint);
            }

            // Update the environment with the refined interval
            if (refinedInterval != null && !refinedInterval.isBottom()) {
                return environment.putState(leftId, refinedInterval);
            }
        } catch (Exception e) {
            // If anything goes wrong, return environment unchanged
            return environment;
        }

        return environment;
    }
}