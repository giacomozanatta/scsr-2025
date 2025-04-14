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

	private static final TaintThreeLevels TOP = new TaintThreeLevels(0);
	private static final TaintThreeLevels TAINTED = new TaintThreeLevels(1);
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels(2);
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels(3);

	private final int state;

	public TaintThreeLevels() {
		this(0);
	}

	private TaintThreeLevels(int state) {
		this.state = state;
	}

	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		if (this == other)
			return this;
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if ((this == TAINTED && other == CLEAN) || (this == CLEAN && other == TAINTED))
			return TOP;
		if (this == TOP || other == TOP)
			return TOP;
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		if (this == other)
			return true;
		if (this == BOTTOM)
			return true;
		if (other == TOP)
			return true;
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
		return this == TAINTED || this == CLEAN || this == TOP;
	}

	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		return left.lub(right);
	}

	@Override
	public StructuredRepresentation representation() {
		if (this == BOTTOM)
			return Lattice.bottomRepresentation();
		if (this == TOP)
			return Lattice.topRepresentation();
		if (this == CLEAN)
			return new StringRepresentation("_");
		return new StringRepresentation("#");
	}
}