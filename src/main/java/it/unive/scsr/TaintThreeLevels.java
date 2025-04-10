package it.unive.scsr;


import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;


public class TaintThreeLevels extends BaseTaint<TaintThreeLevels>  {

	private static final TaintThreeLevels TOP = new TaintThreeLevels((byte) 3);
	private static final TaintThreeLevels TAINTED = new TaintThreeLevels((byte) 2);
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels((byte) 1);
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels((byte) 0);

	private final int tainted;

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

	public TaintThreeLevels() {
		this((byte) 3);
	}

	private TaintThreeLevels(
			byte v) {
		this.tainted = v;
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
		if (left == TAINTED || right == TAINTED)
			return TAINTED;

		if (left == TOP || right == TOP)
			return TOP;

		return CLEAN;
		// TODO: to implement
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
		//return null;
	}
	
}
