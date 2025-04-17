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

	/*
	 * Lattice of Taint Domain with three levels:
	 *
	 *       TOP
	 *     /     \
	 *  TAINT   CLEAN
	 *     \     /
	 *      BOTTOM
	 *
	 * Element meanings:
	 * - TOP: might be tainted or clean
	 * - TAINT: definitely tainted
	 * - CLEAN: definitely clean
	 * - BOTTOM: error state
	 */

	private static final TaintThreeLevels TOP = new TaintThreeLevels("TOP");
	private static final TaintThreeLevels TAINT = new TaintThreeLevels("TAINT");
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels("CLEAN");
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels("BOTTOM");

	private final String level;

	TaintThreeLevels(String level) {
		this.level = level;
	}

	TaintThreeLevels() {
		this("BOTTOM");
	}

	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		if (this == BOTTOM || other == BOTTOM) {
			return other == BOTTOM ? this : other;
		}
		if (this == TOP || other == TOP) {
			return TOP;
		}
		if ((this == TAINT && other == CLEAN) || (this == CLEAN && other == TAINT)) {
			return TOP;
		}
		return this == other ? this : TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		if (this == BOTTOM || other == TOP) {
			return true;
		}
		if (this == TOP) {
			return false;
		}
		return this == other;
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

	@Override
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		if (left == BOTTOM || right == BOTTOM) {
			return BOTTOM;
		}
		if (left == TOP || right == TOP) {
			return TOP;
		}
		if (left == TAINT || right == TAINT) {
			return TAINT;
		}
		return CLEAN;
	}

	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	public StructuredRepresentation representation() {
		return switch (level) {
			case "BOTTOM" -> Lattice.bottomRepresentation();
			case "TOP" -> Lattice.topRepresentation();
			case "CLEAN" -> new StringRepresentation("_");
			case "TAINT" -> new StringRepresentation("#");
			default -> throw new IllegalStateException("Unexpected value: " + level);
		};
	}
}