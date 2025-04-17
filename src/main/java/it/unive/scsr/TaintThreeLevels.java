package it.unive.scsr;


import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.analysis.taint.ThreeLevelsTaint;
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

	public static final TaintThreeLevels TOP = new TaintThreeLevels("TOP");

	public static final TaintThreeLevels TAINTED = new TaintThreeLevels("TAINTED");

	public static final TaintThreeLevels CLEAN = new TaintThreeLevels("CLEAN");

	public static final TaintThreeLevels BOTTOM = new TaintThreeLevels("BOTTOM");

	private final String tainted;

	public TaintThreeLevels() {
		this("TOP");
	}

	public TaintThreeLevels(String tainted) {
		this.tainted = tainted;
	}
	
	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		// TODO: to implement
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		// TODO: to implement
		return false;
	}

	@Override
	public TaintThreeLevels top() {
		// TODO: to implement
		return TOP;
	}

	@Override
	public TaintThreeLevels bottom() {
		// TODO: to implement
		return BOTTOM;
	}

	@Override
	protected TaintThreeLevels tainted() {
		// TODO: to implement
		return TAINTED;
	}

	@Override
	protected TaintThreeLevels clean() {
		// TODO: to implement
		return CLEAN;
	}

	@Override
	public boolean isAlwaysTainted() {
		// TODO: to implement
		return this == TAINTED;
	}

	@Override
	public boolean isPossiblyTainted() {
		// TODO: to implement
		return this == TOP;
	}
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// TODO: to implement
		if(left == TAINTED || right == TAINTED)
			return TAINTED;

		if(left == TOP || right == TOP)
			return TOP;

		return CLEAN;
	}
	
	@Override
	public TaintThreeLevels wideningAux(
			TaintThreeLevels other)
			throws SemanticException {
		// TODO: to implement
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
		// return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TaintThreeLevels that = (TaintThreeLevels) o;
		return tainted.equals(that.tainted);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tainted);
	}
}
