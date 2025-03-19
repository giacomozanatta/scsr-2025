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

public class Parity

    // IMPLEMENTATION NOTE:
    // the code below is outside of the scope of the course. You can uncomment
    // it to get your code to compile. Be aware that the code is written
    // expecting that you have constants for identifying top, bottom, even and
    // odd elements as we saw for the sign domain: if you name them differently,
    // change also the code below to make it work by just using the name of your
    // choice. If you use methods instead of constants, change == with the
    // invocation of the corresponding method

    // instances of this class are lattice elements such that:
    // - their state (fields) hold the information contained into a single
    // variable
    // - they provide logic for the evaluation of expressions
        implements
                BaseNonRelationalValueDomain<
                        // java requires this type parameter to have this class
                        // as type in fields/methods
                        Parity>

    {

    // as this is a finite lattice, we can optimize by having constant elements
    // for each of them
    private static final Parity TOP = new Parity("TOP");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity BOTTOM = new Parity("BOT");
        // this is just needed to distinguish the elements
    private final String value;

    public Parity() {
        this("TOP");
    }

    public Parity(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null) return false;

        if (getClass() != obj.getClass()) return false;

        Parity other = (Parity) obj;
        return (value == other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
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
    public Parity lubAux(Parity other) throws SemanticException {
        // this and other are always incomparable when we reach here
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Parity other) throws SemanticException {
        // this and other are always incomparable when we reach here
        return false;
    }

    @Override
    public StructuredRepresentation representation() {

        if (this == TOP) return Lattice.topRepresentation();

        if (this == BOTTOM) return Lattice.bottomRepresentation();

        if (this == EVEN) return new StringRepresentation("even");

        return new StringRepresentation("odd");
    }

    // logic for evaluating expressions below

    @Override
    public Parity evalNonNullConstant(
            Constant constant,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {

        if (constant.getValue() instanceof Integer) {
            int v = (Integer) constant.getValue();
            int half = v / 2;
            if (v == half * 2)
                return EVEN;
            else
                return ODD;
        }
        return top();
    }

    @Override
    public Parity evalUnaryExpression(
            UnaryOperator operator,
            Parity arg,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {

        if (operator instanceof NumericNegation)
            // -even = even, -odd = odd. no action needed
            return arg;

        return TOP;
    }

    @Override
    public Parity evalBinaryExpression(
            BinaryOperator operator,
            Parity left,
            Parity right,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {

        if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator) {

            if (left == BOTTOM)
                return right;

            if (right == BOTTOM)
                return left;

            if (right == TOP || left == TOP)
                return TOP;

            if (left.equals(right))
                return EVEN;

            return ODD;

        } else if (operator instanceof MultiplicationOperator) {

            if (right == EVEN || left == EVEN)
                return EVEN;

            if (right == ODD && left == ODD)
                return ODD;

            return TOP;

        } else if (operator instanceof DivisionOperator) {
            return TOP;

        }
        return TOP;
    }

}