package it.unive.scsr;


import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;


public class Taint extends BaseTaint<Taint> {

	/*
	 * Lattice of Taint Domain 
	 * 
	 * T
	 * |
	 * C
	 * |
	 * BOTTOM
	 * 
	 */
	private static final Taint TAINT = new Taint(true); // Tainted element (i.e. might be tainted)
	private static final Taint CLEAN = new Taint(false);// Clean element (i.e. definitly clean)
	private static final Taint BOTTOM = new Taint(null);// Bottom element (i.e. error state)
	
	
	private Boolean isTainted; // encoding: true == "tainted", false == "clean", null == "bottom"
	
	public Taint() {
		this(true);
	}
	
	public Taint(Boolean taint) {
		this.isTainted = taint;
		
	}
	
	@Override
	public Taint lubAux(Taint other) throws SemanticException {
		return TAINT;
	}

	@Override
	public boolean lessOrEqualAux(Taint other) throws SemanticException {
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
		return this == BOTTOM ? Lattice.bottomRepresentation()
					: this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");

	}

	
	@Override
	public Taint wideningAux(Taint other) throws SemanticException {
		
		return TAINT;
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
	public boolean isAlwaysTainted() { // false because might be tainted and it is not definitly tainted
		return false;
	}

	@Override
	public boolean isPossiblyTainted() {
		
		return this == TAINT;
	}

}
