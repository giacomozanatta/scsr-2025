package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class TaintThreeLevels extends BaseTaint<TaintThreeLevels> {


	private static final TaintThreeLevels TOP = new TaintThreeLevels(null);  // May be tainted or clean
	private static final TaintThreeLevels TAINT = new TaintThreeLevels(true); // Definitely tainted
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels(false); // Definitely clean
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels(null); // Error state

	private final Boolean isTainted;


	private TaintThreeLevels(Boolean isTainted) {
		this.isTainted = isTainted;
	}


	public TaintThreeLevels() {
		this(null);
	}

	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		if (this == BOTTOM) return other;
		if (other == BOTTOM) return this;
		if (this == TOP || other == TOP) return TOP;
		if (this != other) return TOP;
		return this;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		if (this == other) return true; // Equal elements
		if (this == BOTTOM) return true; // BOTTOM is less than or equal to anything
		if (other == TOP) return true;
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
			SemanticOracle oracle) throws SemanticException {
		if (left == BOTTOM || right == BOTTOM)
			return BOTTOM;

		if (left == TAINT || right == TAINT)
			return TAINT;

		if (left == CLEAN && right == CLEAN)
			return CLEAN;

		return TOP;
	}

	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	public StructuredRepresentation representation() {
		if (this == BOTTOM) return Lattice.bottomRepresentation();
		if (this == TOP) return Lattice.topRepresentation();
		if (this == CLEAN) return new StringRepresentation("_");
		if (this == TAINT) return new StringRepresentation("#");
		return new StringRepresentation("?");
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		TaintThreeLevels other = (TaintThreeLevels) obj;
		return this.isTainted == other.isTainted;
	}

	@Override
	public int hashCode() {
		return isTainted != null ? isTainted.hashCode() : 0;
	}
}
