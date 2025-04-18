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
	 * Lattice of Taint Domain with three level
	 * 
	 * TOP
	 * / \
	 * TAINT CLEAN
	 * \ /
	 * BOTTOM
	 * 
	 * 
	 * Element meanings:
	 * - TOP: might be tainted or clean
	 * - TAINT: definitly tainted
	 * - CLEAN: definitly clean
	 * - BOTTOM: error state
	 * 
	 */

	private static final TaintThreeLevels TOP = new TaintThreeLevels(State.TOP);
	private static final TaintThreeLevels TAINT = new TaintThreeLevels(State.TAINT);
	private static final TaintThreeLevels CLEAN = new TaintThreeLevels(State.CLEAN);
	private static final TaintThreeLevels BOTTOM = new TaintThreeLevels(State.BOTTOM);

	private enum State {
		TOP, // tainted o clean
		TAINT, // tainted
		CLEAN, // clean
		BOTTOM // err
	}

	private final State state;

	private TaintThreeLevels(State state) {
		this.state = state;
	}

	public TaintThreeLevels() {
		this(State.TOP);
	}

	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {

		if (this.state == State.BOTTOM)
			return other;
		if (other.state == State.BOTTOM)
			return this;

		if (this.state == State.TOP || other.state == State.TOP)
			return TOP;

		if ((this.state == State.TAINT && other.state == State.CLEAN) ||
				(this.state == State.CLEAN && other.state == State.TAINT))
			return TOP;

		return this;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		if (this.state == State.BOTTOM)
			return true;

		if (other.state == State.TOP)
			return true;

		if (this.state == State.TAINT || this.state == State.CLEAN)
			return this.state == other.state || other.state == State.TOP;

		return this.state == other.state;
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
		return this.state == State.TAINT;
	}

	@Override
	public boolean isPossiblyTainted() {
		return this.state == State.TAINT || this.state == State.TOP;
	}

	@Override
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return left.lub(right);
	}

	@Override
	public TaintThreeLevels wideningAux(
			TaintThreeLevels other)
			throws SemanticException {
		return lubAux(other);
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
		switch (this.state) {
			case BOTTOM:
				return Lattice.bottomRepresentation();
			case TOP:
				return Lattice.topRepresentation();
			case CLEAN:
				return new StringRepresentation("_"); // clean = _
			case TAINT:
				return new StringRepresentation("#"); // tainted = #
			default:
				return new StringRepresentation("?");
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaintThreeLevels other = (TaintThreeLevels) obj;
		return state == other.state;
	}

	@Override
	public int hashCode() {
		return state.hashCode();
	}
}
