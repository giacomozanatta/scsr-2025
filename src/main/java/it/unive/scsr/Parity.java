package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.parity.BinaryFunctions;

import java.util.Optional;

// IMPLEMENTATION NOTE:
// The code is written expecting to have constants for identifying top, bottom, even and odd elements.
public class Parity implements
        BaseNonRelationalValueDomain<Parity>  {

    // Instead of having an empty constructor, constants are public and accessible from outside.
    public final static Parity TOP = new Parity();
    public final static Parity EVEN = new Parity();
    public final static Parity ODD = new Parity();
    public final static Parity BOTTOM = new Parity();

    @Override
    public Parity lubAux(Parity other) throws SemanticException {
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(Parity other) throws SemanticException {
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
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return Optional
                .of(constant.getValue())
                .map(x -> x instanceof Integer ? (Integer) x : null)
                .map(x -> x % 2 == 0)
                .map(isEven -> isEven ? EVEN : ODD)
                .orElse(top());
    }

    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return Optional
                .of(operator)
                .map(x -> x instanceof NumericNegation)
                .map(isNegation -> isNegation ? arg : null)
                .orElse(top());
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        return Optional
                .ofNullable(BinaryFunctions.INSTANCE.findBy(operator))
                .map(f -> f.apply(left, right))
                .orElse(top());
    }
}