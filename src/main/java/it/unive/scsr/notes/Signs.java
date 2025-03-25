package it.unive.scsr.notes;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalTypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;
import java.util.Set;

public class Signs implements BaseNonRelationalTypeDomain<Signs> {

    private static final Signs TOP = new Signs("TOP");
    private static final Signs BOTTOM = new Signs("BOT");
    private static final Signs ZERO = new Signs("ZERO");
    private static final Signs POS = new Signs("POS");
    private static final Signs NEG = new Signs("NEG");

    private final String sign;

    public Signs(String sign) {
        this.sign = sign;
    }

    public Signs() {
        this.sign = "TOP";
    }

    @Override
    public Signs lubAux(Signs other) {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(Signs other) throws SemanticException {
        return false;
    }

    @Override
    public StructuredRepresentation representation() {
        if (this == TOP) {
            return Lattice.topRepresentation();
        }
        if (this == BOTTOM) {
            return Lattice.bottomRepresentation();
        }
        if (this == POS) {
            return new StringRepresentation("+");
        }
        if (this == NEG) {
            return new StringRepresentation("-");
        }

        return new StringRepresentation("0");
    }

    @Override
    public Signs top() {
        return TOP;
    }

    @Override
    public Signs bottom() {
        return BOTTOM;
    }

    @Override
    public Signs evalNonNullConstant(Constant constant,
                                     ProgramPoint pp,
                                     SemanticOracle oracle) throws SemanticException {
        if (constant.getValue() instanceof Integer v) {
            if (v > 0) {
                return POS;
            } else if (v < 0) {
                return NEG;
            } else {
                return ZERO;
            }

        }

        // For all other cases
        return TOP;
    }

    public Signs negate() {
        if (this == POS) {
            return NEG;
        } else if (this == NEG) {
            return POS;
        } else {
            // Negating zero, top or bottom results in the same value
            return this;
        }
    }

    @Override
    public Signs evalBinaryExpression(BinaryOperator operator, Signs left, Signs right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
        if (operator instanceof AdditionOperator) {
            if (left == NEG) {
                // Sum of negative and zero is negative
                // Sum of negative and negative is negative
                if (right == ZERO || right == NEG) {
                    return NEG;
                } else {
                    // We don't know whether the result is positive or negative
                    return TOP;
                }
            } else if (left == POS) {
                // Sum of positive and zero is positive
                // Sum of positive and positive is positive
                if (right == ZERO || right == POS) {
                    return POS;
                } else {
                    return TOP;
                }
            } else if (left == ZERO) {
                // Sum of zero and any number is the number itself, so
                // it keeps the sign of the right operand
                return right;
            } else {
                return TOP;
            }
        } else if (operator instanceof SubtractionOperator) {
            if (left == NEG) {
                // Subtraction of negative and zero is negative
                // Subtraction of negative and positive is negative
                if (right == ZERO || right == POS) {
                    return NEG;
                } else {
                    return TOP;
                }
            } else if (left == POS) {
                // Subtraction of positive and zero is positive
                // Subtraction of positive and negative is positive
                if (right == ZERO || right == NEG) {
                    return POS;
                } else {
                    return TOP;
                }
            } else if (left == ZERO) {
                return right.negate();
            } else {
                return TOP;
            }
        } else if (operator instanceof MultiplicationOperator) {
            if (left == NEG) {
                return right.negate();
            } else if (left == POS) {
                return right;
            } else {
                return TOP;
            }
        } else if (operator instanceof DivisionOperator) {
            if (right == ZERO) {
                return BOTTOM;
            }

            if (left == NEG) {
                return right.negate();
            } else if (left == POS) {
                return right;
            } else if (left == ZERO) {
                return ZERO;
            } else {
                return TOP;
            }
        }

        return TOP;
    }


    @Override
    public Signs evalUnaryExpression(
            UnaryOperator operator,
            Signs arg,
            ProgramPoint pp,
            SemanticOracle oracle) throws SemanticException {
        if (operator instanceof NumericNegation) {
            return arg.negate();
        }

        return TOP;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Signs signs = (Signs) o;
        return Objects.equals(sign, signs.sign);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sign);
    }

    @Override
    public Set<Type> getRuntimeTypes() {
        // This is the default implementation
        return Set.of();
    }
}
