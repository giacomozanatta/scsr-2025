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
	private static final TaintThreeLevels TOP = new TaintThreeLevels(TaintStates.TOP);
	private static final TaintThreeLevels TAINT = new TaintThreeLevels(TaintStates.TAINT);
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels(TaintStates.CLEAN);
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels(TaintStates.BOTTOM);

	private enum TaintStates {BOTTOM, CLEAN, TAINT, TOP}
	private TaintStates taintState; //0 - BOTTOM, 1 - CLEAN, 2 - TAINT, 3 - TOP

	public TaintThreeLevels() {
		this(TaintStates.TAINT);
	}

	public TaintThreeLevels(TaintStates taintState) {
		this.taintState = taintState;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TaintThreeLevels that)) return false;
        return taintState == that.taintState;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(taintState);
	}

	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		if(other.taintState == taintState)
		{
			if(this.taintState == TaintStates.BOTTOM) return BOTTOM;
			if(this.taintState == TaintStates.TOP) return TOP;
			if(this.taintState == TaintStates.CLEAN) return CLEAN;
			if(this.taintState == TaintStates.TAINT) return TAINT;
		}
		if(this.taintState == TaintStates.TOP || other.taintState == TaintStates.TOP)
			return TOP;
		if((this.taintState == TaintStates.CLEAN && other.taintState == TaintStates.TAINT)
				||(this.taintState == TaintStates.TAINT && other.taintState == TaintStates.CLEAN))
			return TOP;
		if((this.taintState == TaintStates.CLEAN && other.taintState == TaintStates.BOTTOM)
				||(this.taintState == TaintStates.BOTTOM && other.taintState == TaintStates.CLEAN))
			return CLEAN;
		if((this.taintState == TaintStates.TAINT && other.taintState == TaintStates.BOTTOM)
				||(this.taintState == TaintStates.BOTTOM && other.taintState == TaintStates.TAINT))
			return TAINT;
		return null;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		if (other.taintState == taintState)
			return true;
		if (this == BOTTOM || other == TOP)
			return true;
		if (this == TOP || other == BOTTOM)
			return false;
		if((this == CLEAN && other == TAINT)||(this == TAINT && other == CLEAN))
			return false;
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
		return TAINT;
	}

	@Override
	protected TaintThreeLevels clean() {
		return CLEAN;
	}

	@Override
	public boolean isAlwaysTainted() {
        return this.taintState == TaintStates.TAINT;
    }

	@Override
	public boolean isPossiblyTainted() {
        return this.taintState == TaintStates.TOP || this.taintState == TaintStates.TAINT;
    }
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if(left.taintState == TaintStates.BOTTOM || right.taintState == TaintStates.BOTTOM)
			return BOTTOM;
		if(left.taintState == TaintStates.TAINT || right.taintState == TaintStates.TAINT)
			return TAINT;
		if(left.taintState == TaintStates.TOP || right.taintState == TaintStates.TOP)
			return TOP;
		if(left.taintState == TaintStates.CLEAN && right.taintState == TaintStates.CLEAN)
			return CLEAN;
		return null;
	}
	
	@Override
	public TaintThreeLevels wideningAux(
			TaintThreeLevels other)
			throws SemanticException {
		return TAINT;
	}


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
		return this == BOTTOM ? Lattice.bottomRepresentation() : this == TOP ? Lattice.topRepresentation() : this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");
		//return null;
	}
	
}
