package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
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

public class Parity         implements
        BaseNonRelationalValueDomain<
                // java requires this type parameter to have this class
                // as type in fields/methods
                Parity> {

    // as this is a finite lattice, we can optimize by having constant elements
    // for each of them
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity BOTTOM = new Parity("BOT");
    private static final Parity TOP = new Parity("TOP");

    private final String Parity;

    public Parity() {
        this("TOP");
    }

    public Parity(
            String parity) {
        this.Parity = parity;
    }

    @Override
    public it.unive.scsr.Parity lubAux(it.unive.scsr.Parity parity) throws SemanticException {
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(it.unive.scsr.Parity parity) throws SemanticException {
        return false;
    }

    @Override
    public it.unive.scsr.Parity top() {
        return TOP;
    }

    @Override
    public it.unive.scsr.Parity bottom() {
        return BOTTOM;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parity parity = (Parity) o;
        return Objects.equals(Parity, parity.Parity);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Parity);
    }
    @Override
    public StructuredRepresentation representation() {
        if(this==TOP) return Lattice.topRepresentation();
        if(this==BOTTOM) return Lattice.bottomRepresentation();
        if(this==ODD) return new StringRepresentation( "ODD");
        return new StringRepresentation( "EVEN");
    }


    @Override
    public it.unive.scsr.Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if(constant.getValue() instanceof Integer){
            int val=(Integer)constant.getValue();
            if(val%2==0)return EVEN;
            else return ODD;
        }
        return TOP;
    }

    @Override
    public it.unive.scsr.Parity evalUnaryExpression(UnaryOperator operator, it.unive.scsr.Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof NumericNegation)
            return arg;
        return TOP;
    }

    @Override
    public it.unive.scsr.Parity evalBinaryExpression(BinaryOperator operator, it.unive.scsr.Parity left, it.unive.scsr.Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if(left.isTop() || right.isTop())return TOP;
        if(operator instanceof AdditionOperator || operator instanceof SubtractionOperator){
            if(left==right)return EVEN;
            return ODD;
        }
        if(operator instanceof MultiplicationOperator ){
            if(left==EVEN || right==EVEN)return EVEN; 
            return ODD;
        }
        // division is not predictable, always return TOP
        return TOP;
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
}