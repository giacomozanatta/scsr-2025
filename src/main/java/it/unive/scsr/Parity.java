package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;

public class Parity
    implements BaseNonRelationalValueDomain<Parity> {

    public static final Parity TOP = new Parity("TOP");
    public static final Parity EVEN = new Parity("EVEN");
    public static final Parity ODD = new Parity("ODD");
    public static final Parity BOTTOM = new Parity("BOTTOM");

    public final String parity;

    public Parity() {
        this("TOP");
    }

    protected Parity(String parity) {
        this.parity = parity;
    }

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer intVal) {
            if ((intVal & 1) == 0)
                return EVEN;
            else
                return ODD;
        }
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
    public Parity top() {
        return TOP;
    }

    @Override
    public Parity bottom() {
        return BOTTOM;
    }

    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that you have constants for identifying top, bottom, even and
    // odd elements as we saw for the sign domain: if you name them differently,
    // change also the code below to make it work by just using the name of your
    // choice. If you use methods instead of constants, change == with the
    // invocation of the corresponding method

	@Override
	public StructuredRepresentation representation() {
		if (this == TOP)
			return Lattice.topRepresentation();
		if (this == BOTTOM)
			return Lattice.bottomRepresentation();
		if (this == EVEN)
			return new StringRepresentation("EVEN");
		return new StringRepresentation("ODD");
	}

    @Override
    public Parity evalBinaryExpression(BinaryOperator op, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        // guaranteed that neither `left` or `right` are bottom

        if (op instanceof AdditionOperator || op instanceof SubtractionOperator) {
            if (left.isTop() || right.isTop())
                return TOP;
            if (left.equals(right))
                return EVEN;
            else
                return ODD;
        } else if (op instanceof MultiplicationOperator) {
            if (left.equals(EVEN) || right.equals(EVEN))
                return EVEN;
            if (left.equals(ODD) && right.equals(ODD))
                return ODD;
            return TOP;
        } else if (op instanceof DivisionOperator) {
            return TOP; // integer division is annoying :(
            // e.g.:
            // 4 / 2 = 2    Even Even → Even
            // 6 / 4 = 1    Even Even → Odd
            // 9 / 3 = 3    Odd  Odd  → Odd
            // 7 / 3 = 2    Odd  Odd  → Even
        }
        return TOP;
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator op, Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (op instanceof NumericNegation) {
            return arg; // negation preserves parity
        }
        return TOP;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Parity parity1 = (Parity) o;
        return Objects.equals(parity, parity1.parity);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parity);
    }
}