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

    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that you have constants for identifying top, bottom, even and
    // odd elements as we saw for the sign domain: if you name them differently,
    // change also the code below to make it work by just using the name of your
    // choice. If you use methods instead of constants, change == with the
    // invocation of the corresponding method

    private static final Parity TOP = new Parity("TOP");
    private static final Parity BOTTOM = new Parity("BOTTOM");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");

    // Operations only for integer numbers
    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(
            String parity) {
        this.parity = parity;
    }

    @Override
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Parity other = (Parity) obj;
        return Objects.equals(parity, other.parity);
    }

    @Override
    public int hashCode() {
        return parity.hashCode();
    }

    // Il punto è tornare sempre alla rappresentazione corretta
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

    // DA IMPLEMENTARE

    @Override
    public Parity evalNonNullConstant(Constant constant,
                                      ProgramPoint pp,
                                      SemanticOracle oracle){
        if (constant.getValue() instanceof Integer){
            int value = (Integer) constant.getValue();
            if (value % 2 == 0)
                return EVEN;
            else
                return ODD;
        }
        return top();
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator,
                                      Parity arg,
                                      ProgramPoint pp,
                                      SemanticOracle oracle){
        // Non devo negare come nel sign perchè mi serve il valore assoluto
        if (operator instanceof NumericNegation)
            return arg;
        return top();
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator,
                                       Parity left,
                                       Parity right,
                                       ProgramPoint pp,
                                       SemanticOracle oracle){

        if(left.isTop() || right.isTop())
            return top();

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator){
            if (left.equals(right))
                return EVEN;
            else
                return ODD;
        }

        if (operator instanceof MultiplicationOperator){
            if (left == EVEN || right == EVEN)
                return EVEN;
            else
                return ODD;
        }

        if(operator instanceof DivisionOperator){
            return top();
        }

        return top();
    }


}