package it.unive.scsr;
// Import needed classes
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.program.cfg.ProgramPoint;

// IMPLEMENTATION NOTE:
// the code below is outside the scope of the course. You can uncomment
// it to get your code to compile. Be aware that the code is written
// expecting that you have constants for identifying top, bottom, even and
// odd elements as we saw for the sign domain: if you name them differently,
// change also the code below to make it work by just using the name of your
// choice. If you use methods instead of constants, change == with the
// invocation of the corresponding method


public class Parity implements BaseNonRelationalValueDomain<Parity> {
    // Enum for elements in abstract domain: TOP, BOTTOM, EVEN, ODD
    public enum ParityValue {
        TOP, BOTTOM, EVEN, ODD
    }

    private final ParityValue value;
    // Static instances for possible parity values
    public static final Parity TOP = new Parity(ParityValue.TOP);
    public static final Parity BOTTOM = new Parity(ParityValue.BOTTOM);
    public static final Parity EVEN = new Parity(ParityValue.EVEN);
    public static final Parity ODD = new Parity(ParityValue.ODD);

    public Parity() {
        this(ParityValue.TOP);
    }

    public Parity(ParityValue value) {
        this.value = value;
    }

    @Override
    public Parity top() {
        return TOP;
    }

    @Override
    public Parity bottom() {
        return BOTTOM;
    }
    // Representation method
    @Override
    public StructuredRepresentation representation() {
        return switch (value) {
            case TOP -> Lattice.topRepresentation();
            case BOTTOM -> Lattice.bottomRepresentation();
            case EVEN -> new StringRepresentation("Even");
            case ODD -> new StringRepresentation("Odd");
        };
    }
    // Method to evaluate constant value
    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint PP, SemanticOracle oracle) {
        if (constant.getValue() instanceof Integer i)   // If integer, return parity
            return (i % 2 == 0) ? EVEN : ODD;
        return top();                                   // if not return TOP
    }

    @Override
    public Parity evalNullConstant(ProgramPoint PP, SemanticOracle oracle) {
        return top();
    }

    public boolean isEven() {
        return value == ParityValue.EVEN;
    }

    public boolean isOdd() {
        return value == ParityValue.ODD;
    }
    // Method to evaluate unary expression such as numeric negation
    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint PP, SemanticOracle oracle) {
        if (operator == NumericNegation.INSTANCE)
            return arg;
        return TOP;
    }
    // Method to evaluate binary expressions and support the basic operations
    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint PP, SemanticOracle oracle) {
        if (left.isTop() || right.isTop())
            return TOP;

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator)
            return left.equals(right) ? EVEN : ODD;
        else if (operator instanceof MultiplicationOperator)
            return (left.isEven() || right.isEven()) ? EVEN : ODD;
        else if (operator instanceof DivisionOperator)
            return left.isOdd() ? (right.isOdd() ? ODD : EVEN) : (right.isOdd() ? EVEN : TOP);
        else if (operator instanceof ModuloOperator || operator instanceof RemainderOperator)
            return TOP;

        return TOP;
    }

    @Override
    public Parity lubAux(Parity other) throws SemanticException {
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Parity other) throws SemanticException {
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Parity other = (Parity) obj;
        return value == other.value;
    }
    // Method to update the environment
    @Override
    public ValueEnvironment<Parity> assumeBinaryExpression(
            ValueEnvironment<Parity> environment,
            BinaryOperator operator,
            ValueExpression left,
            ValueExpression right,
            ProgramPoint origin,
            ProgramPoint dest,
            SemanticOracle oracle) throws SemanticException {

        if (operator == ComparisonEq.INSTANCE) {
            if (left instanceof Identifier identifierLeft) {
                Parity eval = eval(right, environment, origin, oracle);
                return eval.equals(bottom()) ? environment.bottom() : environment.putState(identifierLeft, eval);
            }
            if (right instanceof Identifier identifierRight) {
                Parity eval = eval(left, environment, origin, oracle);
                return eval.equals(bottom()) ? environment.bottom() : environment.putState(identifierRight, eval);
            }
        }
        return environment;
    }
}