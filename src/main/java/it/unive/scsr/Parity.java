package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Parity implements BaseNonRelationalValueDomain<Parity> {

    private static final Parity BOTTOM = new Parity("BOTTOM");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity TOP = new Parity("TOP");

    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(String parity) {
        this.parity = parity;
    }

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        if (constant.getValue() instanceof Integer num) {
            return num % 2 == 0 ? EVEN : ODD;
        }

        return TOP;
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        return arg;
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator) {
            if (left == TOP || right == TOP) return TOP;
            else if (left == ODD && right == ODD) return EVEN;
            else if (left == EVEN && right == EVEN) return EVEN;
            else return ODD;

        } else if (operator instanceof MultiplicationOperator) {
            if (left == TOP || right == TOP) return TOP;
            else if (left == ODD && right == ODD) return ODD;
            else if (left == EVEN && right == EVEN) return EVEN;
            else return EVEN;
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

    @Override
    public StructuredRepresentation representation() {
        if (this == TOP) return Lattice.topRepresentation();
        if (this == BOTTOM) return Lattice.bottomRepresentation();
        if (this == EVEN) return new StringRepresentation("EVEN");
        else return new StringRepresentation("ODD");
    }
}