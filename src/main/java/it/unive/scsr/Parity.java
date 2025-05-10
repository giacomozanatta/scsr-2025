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

    private static final Parity BOTTOM = new Parity("BOTTOM");
    private static final Parity TOP = new Parity("TOP");
    private static final Parity ODD = new Parity("ODD");
    private static final Parity EVEN = new Parity("EVEN");

    private final String sign;

    public Parity(String sign) {
        this.sign = sign;
    }

    public Parity() {
        this("TOP");
    }

    @Override
    public Parity lubAux(Parity Parity) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(Parity Parity) throws SemanticException {
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
        if (this == TOP) {
            return Lattice.topRepresentation();
        }
        if (this == BOTTOM) {
            return Lattice.bottomRepresentation();
        }
        if (this == ODD) {
            return new StringRepresentation("ODD");
        }
        if (this == EVEN) {
            return new StringRepresentation("EVEN");
        }
        return new StringRepresentation("NULL");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Parity Parity = (Parity) o;
        return Objects.equals(sign, Parity.sign);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sign);
    }

    @Override
    public Parity evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            int v = ((Integer) constant.getValue());
            if (v % 2 == 0) {
                return EVEN;
            } else if (v % 2 > 0) {
                return ODD;
            }
        }
        return TOP;
    }

    @Override
    public Parity evalBinaryExpression(BinaryOperator operator, Parity left, Parity right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof AdditionOperator) {
            if (left == ODD) {
                if (right == EVEN) {
                    return ODD;
                } else {
                    return EVEN;
                }
            } else if (left == EVEN) {
                if (right == ODD) {
                    return ODD;
                }
            }
        } else if (operator instanceof SubtractionOperator) {
            if (left == ODD) {
                if (right == EVEN) {
                    return ODD;
                } else {
                    return EVEN;
                }
            } else if (left == EVEN) {
                if (right == ODD) {
                    return ODD;
                } else {
                    return EVEN;
                }
            }
        } else if (operator instanceof MultiplicationOperator) {
            if (left == EVEN) {
                if (right == ODD) {
                    return EVEN;
                } else {
                    return ODD;
                }
            } else if (left == ODD) {
                if (right == ODD) {
                    return ODD;
                } else {
                    return EVEN;
                }
            }
        } else if (operator instanceof DivisionOperator) {
            if (left == ODD) {
                if (right == ODD) {
                    return ODD;
                } else {
                    return EVEN;
                }
            } else if (left == EVEN) {
                if (right == ODD) {
                    return EVEN;
                } else {
                    return EVEN;
                }
            }
        }
        return TOP;
    }
    public Parity negate(){
        if (this == ODD){
            return ODD;
        }
        if (this == EVEN){
            return EVEN;
        }
        return this;
    }
    @Override
    public Parity evalUnaryExpression(UnaryOperator operator, Parity arg, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof NumericNegation) {
            return arg.negate();
        }
        return TOP;
    }
}
