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

public class Parity implements BaseNonRelationalValueDomain<Parity> {
    private static final Parity BOTTOM = new Parity("BOT");
    private static final Parity TOP = new Parity("TOP");
    private static final Parity ZERO = new Parity("ZERO");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity EVEN = new Parity("EVEN");

    private final String parity;

    public Parity(String parity) {
        this.parity = parity;
    }

    public Parity() {
        this("TOP");
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Parity parity1 = (Parity) o;
        return Objects.equals(parity, parity1.parity);
    }

    @Override
    public Parity lubAux(Parity parity) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(Parity parity) throws SemanticException {
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

    @Override
    public StructuredRepresentation representation() {
        if (this == TOP)
            return Lattice.topRepresentation();
        if (this == BOTTOM)
            return Lattice.bottomRepresentation();
        if (this == EVEN)
            return new StringRepresentation("EVEN");
        if (this == ODD)
            return new StringRepresentation("ODD");
        return new StringRepresentation("0");
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parity);
    }

    @Override
    public Parity evalNonNullConstant(
            Constant constant,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            int v = ((Integer) constant.getValue());
            if (v % 2 == 0)
                return Parity.EVEN;
            else if (v % 2 == 1)
                return Parity.ODD;
            else
                return ZERO;
        }
        return top();
    }

    private Parity negate() {
        if (this == ODD)
            return EVEN;
        else if (this == EVEN)
            return ODD;
        else
            return this;
    }

    @Override
    public Parity evalUnaryExpression(
            UnaryOperator operator,
            Parity arg,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        if (operator instanceof NumericNegation)
            return arg.negate();

        return TOP;
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof AdditionOperator) {
            if (left == EVEN) {
                if (right == ZERO || right == EVEN) {
                    return EVEN;
                } else {
                    return ODD;
                }
            } else if (left == ODD) {
                if (right == ZERO || right == ODD) {
                    return ODD;
                } else {
                    return EVEN;
                }
            }
        } else if (operator instanceof SubtractionOperator) {
            if (left == EVEN) {
                if (right == ZERO || right == EVEN) {
                    return EVEN;
                } else {
                    return ODD;
                }
            } else if (left == ODD) {
                if (right == ZERO || right == ODD) {
                    return ODD;
                } else {
                    return EVEN;
                }
            }
        } else if (operator instanceof MultiplicationOperator) {
            if (left == ZERO || right == ZERO) {
                return ZERO;
            } else if (left == EVEN || right == EVEN) {
                return EVEN;
            } else if (left == ODD && right == ODD) {
                return ODD;
            } else {
                return TOP;
            }
        } else if (operator instanceof DivisionOperator) {
            if (right == ZERO) {
                return BOTTOM;
            } else if (left == EVEN) {
                if (right == EVEN) {
                    return EVEN;
                }
                if (right == ODD) {
                    return BOTTOM;
                }
                if (right == ZERO) {
                    return ZERO;
                }
            }
            return TOP;
        }
        return TOP;
    }
}

// IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that you have constants for identifying top, bottom, even and
    // odd elements as we saw for the sign domain: if you name them differently,
    // change also the code below to make it work by just using the name of your
    // choice. If you use methods instead of constants, change == with the
    // invocation of the corresponding method

//	@Override
//	public StructuredRepresentation representation() {
//		if (this == TOP)
//			return Lattice.topRepresentation();
//		if (this == BOTTOM)
//			return Lattice.bottomRepresentation();
//		if (this == EVEN)
//			return new StringRepresentation("EVEN");
//		return new StringRepresentation("ODD");
//	}
