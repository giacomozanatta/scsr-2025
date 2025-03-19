package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
// import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;

public class Parity implements BaseNonRelationalValueDomain<Parity> {
    private static final Parity TOP = new Parity("TOP");
    private static final Parity BOTTOM = new Parity("BOTTOM");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity EVEN = new Parity("EVEN");

    // this is just needed to distinguish the elements
    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(String parity) {
        this.parity = parity;
    }

    @Override
    public Parity lubAux(Parity parity) throws SemanticException {
        return TOP;
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
		return new StringRepresentation("ODD");
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

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if(constant.getValue() instanceof Integer){
            int v = (Integer) constant.getValue();
            return v % 2 == 0 ? EVEN : ODD;
        }
        else {
            return TOP;
        }
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if(operator instanceof NumericNegation)
            return arg;
        return TOP;
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if(left.isTop() || right.isTop()){
            return TOP;
        }

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator)
            return left.equals(right) ? EVEN : ODD;

        if (operator instanceof MultiplicationOperator){
            if (left == EVEN || right == EVEN)
                return EVEN;
            else
                return ODD;
        }
//        if (operator instanceof DivisionOperator)
//            return TOP;
        return TOP;
    }
}