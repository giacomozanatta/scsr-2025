package it.unive.scsr;


import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

import java.util.Objects;


public class TaintThreeLevels extends BaseTaint<TaintThreeLevels>  {

    /*
     * Lattice of Taint Domain with three level
     *
     * 	  TOP
     * 	/  	   \
     * TAINT	CLEAN
     *  \      /
     *   BOTTOM
     *
     *
     *   Element meanings:
     *   - TOP: might be tainted or clean
     *   - TAINT: definitly tainted
     *   - CLEAN: definitly clean
     *   - BOTTOM: error state
     *
     */
    private static final TaintThreeLevels TOP = new TaintThreeLevels("TOP");
    private static final TaintThreeLevels TAINTED = new TaintThreeLevels("TAINTED");
    private static final TaintThreeLevels CLEAN = new TaintThreeLevels("CLEAN");
    private static final TaintThreeLevels BOTTOM = new TaintThreeLevels("BOTTOM");

    private final String taint;

    public TaintThreeLevels() {
        this("TOP");
    }
    public TaintThreeLevels(String taint) {
        this.taint = taint;
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
        TaintThreeLevels other = (TaintThreeLevels) obj;
        return Objects.equals(taint, other.taint);
    }

    @Override
    public int hashCode() {
        return taint.hashCode();
    }

    @Override
    public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
        return TOP;
    }

    @Override
    public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
        return false;
    }

    @Override
    public TaintThreeLevels top() {
        return TOP;
    }

    @Override
    public TaintThreeLevels bottom() {
        return BOTTOM;
    }

    @Override
    protected TaintThreeLevels tainted() {
        return TAINTED;
    }

    @Override
    protected TaintThreeLevels clean() {
        return CLEAN;
    }

    @Override
    public boolean isAlwaysTainted() {
        return this == TAINTED;
    }

    @Override
    public boolean isPossiblyTainted() {
        // If top can be Tainted or Clean
        return this == TOP;
    }

    public TaintThreeLevels evalBinaryExpression(
            BinaryOperator operator,
            TaintThreeLevels left,
            TaintThreeLevels right,
            ProgramPoint pp,
            SemanticOracle oracle)
            throws SemanticException {

        // both elements are tainted
        if (left == TAINTED && right == TAINTED)
            return TAINTED;

        if (left == CLEAN && right == CLEAN)
            return CLEAN;

        //if one of the two is TOP or are different
        return TOP;
    }

    @Override
    public TaintThreeLevels wideningAux(
            TaintThreeLevels other)
            throws SemanticException {
        return TOP;
    }


    @Override
    public StructuredRepresentation representation() {
        return this == BOTTOM ? Lattice.bottomRepresentation() : this == TOP ? Lattice.topRepresentation() : this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");
        //return null;
    }

}