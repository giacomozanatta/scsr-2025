package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Parity implements BaseNonRelationalValueDomain<Parity> {

    public static final Parity TOP = new Parity((byte) 0);
    public static final Parity ODD = new Parity((byte) 2);
    public static final Parity EVEN = new Parity((byte) 3);
    public static final Parity BOTTOM = new Parity((byte) 1);

    private final byte parity;

    public Parity() {
        this((byte) 0);
    }

    public Parity(byte parity) {
        this.parity = parity;
    }

    @Override
    public Parity lubAux(Parity other) throws SemanticException {
        if (this.equals(other))
            return this;
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Parity other) throws SemanticException {
        return this.equals(other) || other.equals(TOP);
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
    public Parity evalNullConstant(ProgramPoint pp, SemanticOracle oracle) {
        return top();
    }

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) {
        if (constant.getValue() instanceof Integer) {
            int value = (Integer) constant.getValue();
            return value % 2 == 0 ? EVEN : ODD;
        }
        return top();
    }

    public boolean isEven() {
        return this == EVEN;
    }

    public boolean isOdd() {
        return this == ODD;
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle) {
        if (operator == NumericNegation.INSTANCE)
            return arg;
        return top();
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) {
        if (left.equals(TOP) || right.equals(TOP))
            return TOP;

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator) {
            if (left.equals(right))
                return EVEN;
            return ODD;
        } 
        else if (operator instanceof MultiplicationOperator) {
            if (left.isEven() || right.isEven())
                return EVEN;
            return ODD;
        } 
        else if (operator instanceof DivisionOperator) {
            if (right.isEven())
                return TOP;
            return left;
        } 
        else if (operator instanceof ModuloOperator) {
            if (right.isEven())
                return left;
            return TOP;
        }

        return TOP;
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parity)) return false;
        Parity other = (Parity) o;
        return parity == other.parity;
    }

    @Override
    public int hashCode() {
        return parity;
    }
}