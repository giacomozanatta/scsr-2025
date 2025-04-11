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

	/*
	 * Finite lattice of Taint Domain with three level
	 *
	 *		TOP
	 *		/ \
	 * 	TAINT  CLEAN
	 *  	\ /
	 *     BOTTOM
	 *
	 * Element meanings:
	 *	- TOP: might be tainted or clean
	 *  - TAINT: definitely tainted
	 *  - CLEAN: definitely clean
	 *  - BOTTOM: error state
	 *
	 */
	public static final TaintThreeLevels TOP = new TaintThreeLevels();
	public static final TaintThreeLevels TAINT = new TaintThreeLevels();
	public static final TaintThreeLevels CLEAN = new TaintThreeLevels();
	public static final TaintThreeLevels BOTTOM = new TaintThreeLevels();

	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {
		// Since TAINT and CLEAN instances cannot be compared, this method will always return TOP.
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		// Since TAINT and CLEAN instances cannot be compared, this method will always return false.
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
		// If this instance represents the TAINT instance, then you need to make sure it is tainted.
		return this == TAINT;
	}

	@Override
	public boolean isPossiblyTainted() {
		// When managing a TOP instance, the abstract domain cannot know for sure whether the path will be tainted or
		// not.
		return this == TOP;
	}
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return super.evalBinaryExpression(operator, left, right, pp, oracle);
	}
	
	@Override
	public TaintThreeLevels wideningAux(
			TaintThreeLevels other)
			throws SemanticException {
		// Since this is a finite lattice, the default implementation applies. As documented "The default implementation
		// of this method delegates to lubAux(BaseLattice), and is therefore safe for finite lattices and ACC."
		return super.wideningAux(other);
	}

	@Override
	public StructuredRepresentation representation() {
		if (this == BOTTOM) return Lattice.bottomRepresentation();
		if (this == TOP) return Lattice.topRepresentation();
		if (this == CLEAN) return new StringRepresentation("_");
		return new StringRepresentation("#");
	}
	
}
