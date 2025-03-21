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

	public static final Parity EVEN = new Parity("EVEN");
	public static final Parity ODD = new Parity("ODD");
	public static final Parity TOP = new Parity("TOP");
	public static final Parity BOTTOM = new Parity("BOTTOM");

	private final String parity;

	public Parity() {
		this("TOP");
	}

	public Parity(String parity) {
		this.parity = parity;
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
	public Parity lubAux(Parity parity) throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(Parity parity) throws SemanticException {
		return false;
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
	public Parity evalNullConstant(ProgramPoint pp,
								   SemanticOracle oracle) throws SemanticException {

		return top();
	}

	@Override
	public Parity evalNonNullConstant(Constant constant, ProgramPoint pp,
									  SemanticOracle oracle) throws SemanticException {

		Object value = constant.getValue();
		if(value instanceof Integer) {
			Integer integer = (Integer) value;
			if(integer % 2 == 0)
				return EVEN;
			else
				return ODD;
		}

		return top();
	}

	@Override
	public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp,
									  SemanticOracle oracle) throws SemanticException {

		//possibile fare if(operator instanceof NumericNegation) ?
		if(operator instanceof NumericNegation)
		//if (operator == NumericNegation.INSTANCE)
			return arg;

		return top();
	}

	@Override
	public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right,
									   ProgramPoint pp, SemanticOracle oracle) throws SemanticException {

		if(left.isTop() || right.isTop())
			return top();

		if(operator instanceof AdditionOperator || operator instanceof SubtractionOperator) {
			if(right.equals(left))
				return EVEN;
			else
				return ODD;
		} else if(operator instanceof MultiplicationOperator) {
			if(left.isEven() || right.isEven())
				return EVEN;
			else
				return ODD;
		} else if(operator instanceof DivisionOperator) {
			if (left.isOdd()) {
				if(right.isOdd())
					return ODD;
				else
					return EVEN;
			} else {
				if(right.isOdd())
					return EVEN;
				else
					return TOP;
			}
		} else if(operator instanceof ModuloOperator || operator instanceof RemainderOperator)
			return TOP;

		return TOP;
	}

	public boolean isEven() {
		return this == EVEN;
	}

	public boolean isOdd() {
		return this == ODD;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Parity other = (Parity) o;
		return Objects.equals(parity, other.parity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parity);
	}
}