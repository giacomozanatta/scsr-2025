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
    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(String parity) {
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

    private static final Parity BOTTOM = new Parity("BOT");
    private static final Parity TOP = new Parity("TOP");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Parity parity_other = (Parity) obj;
        return Objects.equals(parity, parity_other.parity);
    }

    public int hashCode() {
        return Objects.hash(parity);
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
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            int value = (Integer) constant.getValue();
            if (value % 2 == 0) {
                return EVEN;
            } else {
                return ODD;
            }
        }
        return TOP;
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof NumericNegation) {
            return arg;
        }
        return TOP;
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (left == BOTTOM || right == BOTTOM)
            return BOTTOM;

        if (operator instanceof DivisionOperator)
            return TOP;

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator){
            if (left == right)
                return EVEN;
            else if ((left == EVEN && right == ODD)|| (left == ODD && right == EVEN))
                return ODD;
            else
                return TOP;
        } else if (operator instanceof MultiplicationOperator) {
            if (left == EVEN || right == EVEN)
                return EVEN;
            if (left == ODD && right == ODD)
                return ODD;
            if (left == TOP || right == TOP)
                return TOP;
        }

        return TOP;
    }
}
