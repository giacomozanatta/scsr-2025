package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.taintthreelevels.BinaryFunctions;
import java.util.Optional;

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
		// ...
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
		// ...
		return false;
	}

	@Override
	public TaintThreeLevels top() {
		// ...
		return TOP;
	}

	@Override
	public TaintThreeLevels bottom() {
		// ...
		return BOTTOM;
	}

	@Override
	protected TaintThreeLevels tainted() {
		// ...
		return TAINT;
	}

	@Override
	protected TaintThreeLevels clean() {
		// ...
		return CLEAN;
	}

	@Override
	public boolean isAlwaysTainted() {
		// ...
		return this == TAINT;
	}

	@Override
	public boolean isPossiblyTainted() {
		// ...
		return this == TOP;
	}
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// ...
		return Optional.ofNullable(BinaryFunctions.INSTANCE.findBy(operator))
				.map(f -> f.apply(left, right))
				.orElse(TOP);
	}
	
	@Override
	public TaintThreeLevels wideningAux(
			TaintThreeLevels other)
			throws SemanticException {
		// ... The default implementation of this method delegates to lubAux(BaseLattice), and is thus safe for finite
		// lattices and ACC ones.
		return super.wideningAux(other);
	}

	@Override
	public StructuredRepresentation representation() {
		// ...
		if (this == BOTTOM) return Lattice.bottomRepresentation();
		if (this == TOP) return Lattice.topRepresentation();
		if (this == CLEAN) return new StringRepresentation("_");
		return new StringRepresentation("#");
	}
	
}
