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
	private static final TaintThreeLevels TAINT = new TaintThreeLevels("TAINT");
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels("CLEAN");
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels("BOTTOM");
	private static final TaintThreeLevels TOP = new TaintThreeLevels("TOP");

	private String taintStatus;

	public TaintThreeLevels() {
		this("TOP");
	}

	public TaintThreeLevels(String taintStatus) {
		this.taintStatus = taintStatus;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		TaintThreeLevels that = (TaintThreeLevels) o;
		return Objects.equals(taintStatus, that.taintStatus);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(taintStatus);
	}

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
	
	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		//Since the only case is that this == CLEAN AND other == TAINT or vice versa
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		//Since the only case is that this == CLEAN AND other == TAINT or vice versa
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
		return this == TAINT;
	}

	@Override
	public boolean isPossiblyTainted() {
		return this == TAINT || this == TOP;
	}
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {

		if(left == TAINT || right == TAINT) {
			return TAINT;
		}

		if(left == TOP || right == TOP) {
			return TOP;
		}

		return CLEAN;
	}
	
	@Override
	public TaintThreeLevels wideningAux(
			TaintThreeLevels other)
			throws SemanticException {
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
	
	@Override
	public StructuredRepresentation representation() {
		return this == BOTTOM ? Lattice.bottomRepresentation() : this == TOP ? Lattice.topRepresentation() : this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");
	}
	
}
