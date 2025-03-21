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
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Parity implements BaseNonRelationalValueDomain<Parity> {

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

    public static final Parity TOP = new Parity(0);
    public static final Parity BOTTOM = new Parity(3);

    public static final Parity EVEN = new Parity(2);
    public static final Parity ODD = new Parity(1);

    public final Integer parity;

    public Parity() {
		this.parity = 0;
    }

    public Parity(int parity) {
        this.parity = parity;
    }

    // -----------------------------------------------------------

    @Override
	public Parity bottom() {
		return BOTTOM;
	}

	@Override
	public Parity top() {
		return TOP;
	}

	public boolean isEven(){
		return parity == EVEN.parity;
	}

	public boolean isOdd(){
		return parity == ODD.parity;
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		return prime * result + parity;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		
        if (obj == null || getClass() != obj.getClass())
			return false;

		Parity other = (Parity) obj;
		
        return parity == other.parity;
	}

    // -----------------------------------------------------------

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle s_oracle) throws SemanticException {
		
		if (constant.getValue() instanceof Integer) 
			return ((Integer) constant.getValue()) % 2 == 0 ? EVEN : ODD;

		return TOP;
	}

    @Override
	public Parity evalNullConstant(ProgramPoint pp, SemanticOracle s_oracle) throws SemanticException {
		return TOP;
	}


    @Override
	public boolean lessOrEqualAux(Parity element) throws SemanticException {
		return false;
	}

	@Override
	public Parity lubAux(Parity element) throws SemanticException {
		return TOP;
	}

    @Override
	public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle s_oracle) throws SemanticException {
		if (left.isTop() || right.isTop())
			return TOP;
		
        else if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator)
			return right.equals(left) ? EVEN : ODD;
		
        else if (operator instanceof MultiplicationOperator)
			return left.isEven() || right.isEven() ? EVEN : ODD;

		return TOP; // It handles also DivisionOperator
	}

	@Override
	public Parity evalUnaryExpression(UnaryOperator operator, Parity element, ProgramPoint pp, SemanticOracle s_oracle) throws SemanticException {
		
        return (operator instanceof NumericNegation) ? element : TOP;
	}



}
