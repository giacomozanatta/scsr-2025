package it.unive.scsr;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Taint extends BaseTaint<Taint> {

	private static final Taint TAINT = new Taint(true);   // Tainted
	private static final Taint CLEAN = new Taint(false);  // Clean
	private static final Taint BOTTOM = new Taint(null);  // Bottom

	private final Boolean isTainted;

	public Taint() {
		this(true); // default is tainted
	}

	public Taint(Boolean taint) {
		this.isTainted = taint;
	}

	@Override
	public Taint lubAux(Taint other) throws SemanticException {
		if (this == BOTTOM)
			return other;
		if (other == BOTTOM)
			return this;
		if (this == TAINT || other == TAINT)
			return TAINT;
		return CLEAN;
	}

	@Override
	public boolean lessOrEqualAux(Taint other) throws SemanticException {
		if (this == other)
			return true;
		if (this == BOTTOM)
			return true;
		if (other == TAINT)
			return true;
		if (this == TAINT && other != TAINT)
			return false;
		return false;
	}

	@Override
	public Taint top() {
		return TAINT;
	}

	@Override
	public Taint bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (this == BOTTOM)
			return Lattice.bottomRepresentation();
		if (this == CLEAN)
			return new StringRepresentation("_"); // clean
		return new StringRepresentation("#"); // tainted
	}

	@Override
	public Taint wideningAux(Taint other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected Taint tainted() {
		return TAINT;
	}

	@Override
	protected Taint clean() {
		return CLEAN;
	}

	@Override
	public boolean isAlwaysTainted() {
		return this == TAINT;
	}

	@Override
	public boolean isPossiblyTainted() {
		return this == TAINT || this == CLEAN;
	}
}
