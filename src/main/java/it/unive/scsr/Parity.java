package it.unive.scsr;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Parity implements Lattice<Parity> {
    public static final Parity ODD = new Parity("ODD");
    public static final Parity EVEN = new Parity("EVEN");
    public static final Parity TOP = new Parity("TOP");
    public static final Parity BOTTOM = new Parity("BOTTOM");
    public static final Parity ZERO = new Parity("ZERO");

    private final String name;

    private Parity(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean lessOrEqual(Parity other) throws SemanticException {
        return false;
    }

    @Override
    public Parity lub(Parity other) throws SemanticException {
        return null;
    }

    @Override
    public Parity top() {
        return null;
    }

    @Override
    public Parity bottom() {
        return null;
    }

    @Override
    public StructuredRepresentation representation() {
        return null;
    }

    protected Parity lubAux(Parity other) {
        if (this.equals(other)) return this;
        if (this.equals(BOTTOM)) return other;
        if (other.equals(BOTTOM)) return this;
        return TOP;
    }

    protected boolean lessOrEqualAux(Parity other) {
        if (this.equals(other) || other.equals(TOP)) return true;
        return this.equals(BOTTOM);
    }

    public static Parity evalNonNullConstant(int cst) {
        return (cst % 2 == 0) ? EVEN : ODD;
    }

    public static Parity evalUnaryExpression(UnaryOperator op, Parity arg) {
        return arg; // Negation (-x) does not change parity
    }

    public static Parity evalBinaryExpression(BinaryOperator op, Parity left, Parity right) {
        if (op instanceof AdditionOperator || op instanceof SubtractionOperator) {
            if (left.equals(EVEN) && right.equals(EVEN)) return EVEN; // 2 + 2 = 4 || 4 - 2 = 2 --> EV
            if (left.equals(ODD) && right.equals(ODD)) return EVEN; // 3 + 3 = 6 || 7 - 3 = 4 --> EV
            return ODD; // 4 + 3 = 7 || 4 - 3 = 1 --> ODD
        } else if (op instanceof MultiplicationOperator) {
            if (left.equals(EVEN) || right.equals(EVEN)) return EVEN; // 2 * 6 = 12 --> EV
            return ODD; // 3 * 3 = 9 --> ODD
        } else if (op instanceof DivisionOperator) {
            // Handle division by zero
            if (right.equals(ZERO)) return BOTTOM; // Division by zero is undefined

            // Handle division by even numbers
            if (right.equals(EVEN)) {
                if (left.equals(EVEN)) return TOP; // EVEN / EVEN could be EVEN or ODD
                if (left.equals(ODD)) return TOP;  // ODD / EVEN could be EVEN or ODD
            }

            // Handle division by odd numbers
            if (right.equals(ODD)) {
                return left; // The parity of the result is the same as the left operand
            }
            return TOP;
        } else {
            return TOP;
        }
    }
}