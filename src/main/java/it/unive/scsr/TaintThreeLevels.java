package it.unive.scsr;


import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.taint.BaseTaint;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
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
	 *   - TAINT: definitly tainted
	 *   - CLEAN: definitly clean
	 *   - BOTTOM: error state
	 * 
	 */
	
    private static final TaintThreeLevels BOTTOM = new TaintThreeLevels("BOTTOM");
    private static final TaintThreeLevels TAINT  = new TaintThreeLevels("TAINT");
    private static final TaintThreeLevels CLEAN  = new TaintThreeLevels("CLEAN");
    private static final TaintThreeLevels TOP    = new TaintThreeLevels("TOP");

    private final String level;

    private TaintThreeLevels(String level) {
        this.level = level;
    }

    public TaintThreeLevels() {
        this("TOP");
    }


	@Override
	public TaintThreeLevels lubAux(TaintThreeLevels other) throws SemanticException {

    if (this == bottom())
        return other;
    if (other == bottom())
        return this;

    if (this == top() || other == top())
        return top();

    if ((this == tainted() && other == clean()) || (this == clean() && other == tainted()))
        return top();

    return this;
	}


	@Override
	public boolean lessOrEqualAux(TaintThreeLevels other) throws SemanticException {
    
    if (this == bottom())
        return true;

    if (other == top())
        return true;

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

		return this == tainted();
	}

	@Override
	public boolean isPossiblyTainted() {
    
    	return this == tainted() || this == top();
	}
	
	public TaintThreeLevels evalBinaryExpression(
			BinaryOperator operator,
			TaintThreeLevels left,
			TaintThreeLevels right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
	
    	if (left == bottom() || right == bottom())
        	return bottom();

    	if (left == tainted() || right == tainted())
        	return tainted();

       	if (left == top() || right == top())
        	return top();

    	return clean();
	}
	
	@Override
	public TaintThreeLevels wideningAux(TaintThreeLevels other)
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
		// return this == BOTTOM ? Lattice.bottomRepresentation() : this == TOP ? Lattice.topRepresentation() : this == CLEAN ? new StringRepresentation("_") : new StringRepresentation("#");
		return null;
	}
	
}
