package it.unive.scsr;

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

public class Overflow implements BaseNonRelationalValueDomain<Overflow> {

    private static final Overflow BOTTOM = new Overflow(-1, "BOT");
    private static final Overflow NOK = new Overflow(0, "NOK"); // Not Overflown/Known
    private static final Overflow POV = new Overflow(1, "POV"); // Positive Overflow
    private static final Overflow NOV = new Overflow(2, "NOV"); // Negative Overflow (Underflow)
    private static final Overflow AOU = new Overflow(3, "AOU"); // Ambiguous Overflow/Underflow
    private static final Overflow TOP = new Overflow(4, "TOP");

    private final int level; // Used for comparison in the lattice
    private final String desc;

    public Overflow() {
        this(4, "TOP"); // Default: TOP
    }

    private Overflow(int level, String desc) {
        this.level = level;
        this.desc = desc;
    }

    @Override
    public Overflow top() {
        return TOP;
    }

    @Override
    public boolean isTop() {
        return this == TOP;
    }

    @Override
    public Overflow bottom() {
        return BOTTOM;
    }

    @Override
    public boolean isBottom() {
        return this == BOTTOM;
    }

    @Override
    public Overflow lubAux(Overflow other) throws SemanticException {
        // Lattice structure:
        //      TOP
        //       |
        //      AOU
        //     /   \
        //   POV   NOV
        //     \   /
        //      NOK
        //       |
        //      BOT
        if (this == other) return this;
        if (this.isTop() || other.isTop()) return TOP;
        if (this.isBottom()) return other;
        if (other.isBottom()) return this;

        if (this == AOU || other == AOU) return AOU;

        if ((this == POV && other == NOV) || (this == NOV && other == POV)) return AOU;

        if (this == POV || other == POV) return POV; // lub(POV, NOK) = POV
        if (this == NOV || other == NOV) return NOV; // lub(NOV, NOK) = NOV
        
        // Should be covered by NOK lub NOK = NOK (this == other)
        // or NOK lub anything else handled above.
        return TOP; // Fallback, though should be covered
    }

    @Override
    public boolean lessOrEqualAux(Overflow other) throws SemanticException {
        // Lattice structure:
        //      TOP
        //       |
        //      AOU
        //     /   \
        //   POV   NOV
        //     \   /
        //      NOK
        //       |
        //      BOT
        if (this == other) return true;
        if (this.isBottom() || other.isTop()) return true;
        if (this.isTop() || other.isBottom()) return false;

        if (this == NOK) return true; // NOK is less than or equal to POV, NOV, AOU, TOP
        if (other == AOU) return this == POV || this == NOV || this == NOK;
        if (other == POV) return this == NOK;
        if (other == NOV) return this == NOK;
        
        return false; // e.g. POV is not <= NOV
    }

    @Override
    public StructuredRepresentation representation() {
        return new StringRepresentation(this.desc);
    }

    @Override
    public Overflow evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (constant.getValue() instanceof Integer) {
            return NOK;
        }
        return TOP; // For other types of constants
    }

    @Override
    public Overflow evalUnaryExpression(UnaryOperator operator, Overflow arg, ProgramPoint pp, SemanticOracle oracle)
            throws SemanticException {
        if (arg.isBottom()) return BOTTOM;
        if (arg.isTop()) return TOP;

        if (operator instanceof NumericNegation) {
            if (arg == NOK) return POV; // Conservative for -Integer.MIN_VALUE
            if (arg == POV) return NOV;
            if (arg == NOV) return POV;
            if (arg == AOU) return AOU;
        }
        return TOP; // Default for other unary operators or unhandled states
    }

    @Override
    public Overflow evalBinaryExpression(BinaryOperator operator, Overflow left, Overflow right, ProgramPoint pp,
            SemanticOracle oracle) throws SemanticException {
        if (left.isBottom() || right.isBottom()) return BOTTOM;
        if (left.isTop() || right.isTop()) return TOP;

        // If AOU is an operand, result is AOU (unless other is TOP/BOT already handled)
        if (left == AOU || right == AOU) return AOU;

        if (operator instanceof AdditionOperator ||
            operator instanceof SubtractionOperator ||
            operator instanceof MultiplicationOperator ||
            operator instanceof DivisionOperator) {

            // Both NOK:
            if (left == NOK && right == NOK) {
                if (operator instanceof DivisionOperator) {
                    return POV; // For MIN_INT / -1 case
                }
                return AOU; // For +, -, * on two NOKs
            }

            // One is POV, the other is NOK
            if ((left == POV && right == NOK) || (left == NOK && right == POV)) {
                if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator) return POV;
                // For Multiplication or Division with NOK, sign of NOK matters. Since we don't know, result is AOU.
                return AOU;
            }

            // One is NOV, the other is NOK
            if ((left == NOV && right == NOK) || (left == NOK && right == NOV)) {
                if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator) return NOV;
                // For Multiplication or Division with NOK, sign of NOK matters. Since we don't know, result is AOU.
                return AOU;
            }
            
            // Both are POV
            if (left == POV && right == POV) {
                if (operator instanceof AdditionOperator || operator instanceof MultiplicationOperator) return POV;
                return AOU; // -, / can result in smaller values or change sign context
            }

            // Both are NOV
            if (left == NOV && right == NOV) {
                if (operator instanceof AdditionOperator) return NOV;
                if (operator instanceof MultiplicationOperator) return POV; // neg * neg = pos
                return AOU; // -, /
            }

            // One POV, one NOV
            if ((left == POV && right == NOV) || (left == NOV && right == POV)) {
                 if (operator instanceof SubtractionOperator && left == POV && right == NOV) return POV; // POV - NOV = POV + pos
                 if (operator instanceof SubtractionOperator && left == NOV && right == POV) return NOV; // NOV - POV = NOV + neg
                 if (operator instanceof MultiplicationOperator) { // POV * NOV
                     return NOV;
                 }
                // Other cases (e.g. addition, division) are ambiguous
                return AOU;
            }
        }
        // Default for non-arithmetic binary operators or unhandled combinations
        return TOP;
    }
    
    @Override
    public Overflow wideningAux(Overflow other) throws SemanticException {
        // Standard widening for a finite lattice is the LUB.
        return lubAux(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, desc);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Overflow other = (Overflow) obj;
        return level == other.level && Objects.equals(desc, other.desc);
    }
}
