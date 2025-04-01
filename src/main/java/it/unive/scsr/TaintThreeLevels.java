package it.unive.scsr;

// Import needed classes
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;


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
	 *   - TAINT: definitely tainted
	 *   - CLEAN: definitely clean
	 *   - BOTTOM: error state
	 * 
	 */
	public static final TaintThreeLevels TOP = new TaintThreeLevels("TOP");
	public static final TaintThreeLevels TAINTED = new TaintThreeLevels("TAINTED");
	public static final TaintThreeLevels CLEAN = new TaintThreeLevels("CLEAN");
	public static final TaintThreeLevels BOTTOM = new TaintThreeLevels("BOTTOM");

	private final String level;
// Private constructor
	private TaintThreeLevels(String level) {
		this.level = level;
	}
// Least Upper Bound operations over the lattice
	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) {
		if (this == other) return this;
		if (this == BOTTOM) return other;
		if (other == BOTTOM) return this;
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) {
		if (this == other || other == TOP) return true;
		if (this == TOP) return false;
		return this == BOTTOM;
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
		return this == TAINTED || this == TOP;
	}
// This method evaluates a binary expression
	@Override
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint PP,
			SemanticOracle oracle) throws SemanticException {
		if (left == null || right == null) {
			throw new SemanticException("Operands can't be null");
		}
		if (left == TAINTED || right == TAINTED) return TAINTED;
		if (left == TOP || right == TOP) return TOP;
		return CLEAN;
	}
// This method for widening in the static analysis
	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other) throws SemanticException {
		if (other == null) {
			throw new SemanticException("Widening operand can't be null");
		}
		return lubAux(other); // Simple widening strategy
	}
// ALREADY PRESENT
	@Override
	public StructuredRepresentation representation() {
		if (this == BOTTOM) return Lattice.bottomRepresentation();
		if (this == TOP) return Lattice.topRepresentation();
		return this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");
	}
}
