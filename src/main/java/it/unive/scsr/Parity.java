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

    // ---------------------------------------------------------------------------


    private static final Parity BOTTOM = new Parity("BOT");
    private static final Parity TOP = new Parity("TOP");
    private static final Parity EVEN = new Parity("EVEN");
    private static final Parity ODD = new Parity("ODD");

    // this is just needed to distinguish the elements
    private final String parity;

    public Parity() {
        this("TOP");
    }

    public Parity(String parity) {
        this.parity = parity;
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
    public Parity top() {
        return TOP;
    }

    @Override
    public Parity bottom() {
        return BOTTOM;
    }

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer v) {
            if (v % 2 == 0) {
                return EVEN;
            } else {
                return ODD;
            }
        }

        return top();
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof NumericNegation) {
            // TODO check if this is the expected behaviour
            return arg;
        }

        return TOP;
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        // I use the equals method instead of the == operator to avoid
        // the edge case in which Parity is instantiated with the "new" keyword
        // instead of using the defined constants
        if (
                operator instanceof AdditionOperator
                || operator instanceof SubtractionOperator
        ) {
            if (left.equals(right)) {
                return EVEN;
            } else if (
                    left.equals(EVEN) && right.equals(ODD)
                    || left.equals(ODD) && right.equals(EVEN)
            ) {
                return ODD;
            } else {
                return TOP;
            }
        } else if (operator instanceof MultiplicationOperator) {
            if (
                    left.equals(EVEN) && right.equals(EVEN)
                    || left.equals(EVEN) && right.equals(ODD)
                    || left.equals(ODD) && right.equals(EVEN)
            ) {
                return EVEN;
            } else if (left.equals(ODD) && right.equals(ODD)) {
                return ODD;
            } else {
                return TOP;
            }
        } else if (operator instanceof DivisionOperator) {
            if (left.equals(ODD) && right.equals(EVEN)) {
                // ODD / EVEN: BOTTOM
                return BOTTOM;
            } else {
                // EVEN / EVEN: EVEN, ODD or BOTTOM
                // EVEN / ODD: EVEN or BOTTOM
                // ODD / ODD: ODD or BOTTOM
                return TOP;
            }
        }

        return TOP;
    }
}