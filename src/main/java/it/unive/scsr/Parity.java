package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
//import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Parity 
    implements
        BaseNonRelationalValueDomain<
                // java requires this type parameter to have this class
                // as type in fields/methods
                Parity> {

    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that you have constants for identifying top, bottom, even and
    // odd elements as we saw for the sign domain: if you name them differently,
    // change also the code below to make it work by just using the name of your
    // choice. If you use methods instead of constants, change == with the
    // invocation of the corresponding method
    private static final Parity BOTTOM = new Parity("BOT");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity TOP = new Parity("TOP");

    // this is just needed to distinguish the elements
    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(
            String parity) {
        this.parity = parity;
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
    public boolean equals(
            Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Parity other = (Parity) obj;
        if (!parity.equals(other.parity))
            return false;
        return true;
    }

    @Override
    public Parity top() {
        // the top element of the lattice
        // if this method does not return a constant value,
        // you must override the isTop() method!
        return TOP;
    }

    @Override
    public Parity bottom() {
        // the bottom element of the lattice
        // if this method does not return a constant value,
        // you must override the isBottom() method!
        return BOTTOM;
    }

    @Override
    public Parity lubAux(
            Parity other)
            throws SemanticException {
        // this and other are always incomparable when we reach here
        return top();
    }

    @Override
    public boolean lessOrEqualAux(
            Parity other)
            throws SemanticException {
        // this and other are always incomparable when we reach here
        return false;
    }

    @Override
    public Parity evalNonNullConstant(
            Constant constant,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            Integer v = (Integer) constant.getValue();
            return (v % 2 == 0) ? EVEN : ODD;
        }
        return TOP;
    }

    @Override
    public Parity evalUnaryExpression(
            UnaryOperator operator,
            Parity arg,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {
        return (operator instanceof NumericNegation) ? arg : TOP;
    }

    @Override
    public Parity evalBinaryExpression(
            BinaryOperator operator,
            Parity left,
            Parity right,
            ProgramPoint pp,
            SemanticOracle oracle) throws SemanticException {
        
        // since we are defaulting all non-integer constants to TOP, we need to return TOP
        // if any of the operands is TOP
        if (left == TOP || right == TOP)
            return TOP;

        // Handle addition and subtraction
        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator) 
            // If both operands have the same parity, result is EVEN; otherwise, it's ODD
            return (left.equals(right)) ? EVEN : ODD;

        // Handle multiplication
        if (operator instanceof MultiplicationOperator) {
            if (left == EVEN || right == EVEN)
                return EVEN;
            if (left == ODD && right == ODD)
                return ODD;
        }

        // Default to TOP for any other case, including division
        return TOP;
    }

    @Override
    public int hashCode() {
        return parity.hashCode();
    }
}