package it.unive.scsr;

import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.NegatableOperator;
import it.unive.lisa.symbolic.value.operator.NumericNegation;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals
		// instances of this class are lattice elements such that:
		// - their state (fields) hold the information contained into a single
		// variable
		// - they provide logic for the evaluation of expressions
		implements
		BaseNonRelationalValueDomain<
				// java requires this type parameter to have this class
				// as type in fields/methods
				Intervals>, Comparable<Intervals> {

	/**
	 * The interval represented by this domain element.
	 */
	public final IntInterval interval;
	
	/**
	 * The abstract zero ({@code [0, 0]}) element.
	 */
	public static final Intervals ZERO = new Intervals(IntInterval.ZERO);

	/**
	 * The abstract top ({@code [-Inf, +Inf]}) element.
	 */
	public static final Intervals TOP = new Intervals(IntInterval.INFINITY);

	/**
	 * The abstract bottom element.
	 */
	public static final Intervals BOTTOM = new Intervals(null);

	/**
	 * Builds the interval.
	 * 
	 * @param interval the underlying {@link IntInterval}
	 */
	public Intervals(
			IntInterval interval) {
		this.interval = interval;
	}

	/**
	 * Builds the interval.
	 * 
	 * @param lower  the lower bound
	 * @param upper the higher bound
	 */
	public Intervals(
			MathNumber lower,
			MathNumber upper) {
		this(new IntInterval(lower, upper));
	}

	/**
	 * Builds the interval.
	 * 
	 * @param low  the lower bound
	 * @param high the higher bound
	 */
	public Intervals(
			int low,
			int high) {
		this(new IntInterval(low, high));
	}

	/**
	 * Builds the top interval.
	 */
	public Intervals() {
		this(IntInterval.INFINITY);
	}
	
	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		
		// TODO: The semantics of negation should be implemented here! 
		
		if(operator instanceof NegatableOperator || operator instanceof NumericNegation) {
			
		}
		
		return top();
	}
	
	@Override
	public Intervals glbAux(Intervals other) throws SemanticException {
		
		IntInterval a = this.interval;
		IntInterval b = other.interval;
		
		MathNumber lA = a.getLow();
		MathNumber lB = b.getLow();
		
		MathNumber uA = a.getHigh();
		MathNumber uB = b.getHigh();
		
		if(lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
			return BOTTOM;
		
		MathNumber newLower = lA.max(lB);
		MathNumber newUpper = uA.min(uB);
		
		Intervals newInterval = new Intervals(newLower, newUpper);
		
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
	}

	@Override
	public Intervals lubAux(Intervals other) throws SemanticException {
		
		IntInterval a = this.interval;
		IntInterval b = other.interval;
		
		MathNumber lA = a.getLow();
		MathNumber lB = b.getLow();
		
		MathNumber uA = a.getHigh();
		MathNumber uB = b.getHigh();
		
		MathNumber newLower = lA.min(lB);
		MathNumber newUpper = uA.max(uB);
		
		if(lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
			return BOTTOM;
		
		Intervals newInterval = new Intervals(newLower, newUpper);
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() :
			newInterval;
	}

	@Override
	public boolean lessOrEqualAux(Intervals other) throws SemanticException {
		
		return other.interval.includes(this.interval);
	}


	@Override
	public Intervals top() {
		// the top element of the lattice is [-inf, +inf]
		return TOP;
	}

	@Override
	public boolean isTop() {
		return interval != null && interval.isInfinity();
	}
	
	@Override
	public Intervals bottom() {
		// the bottom element of the lattice is an element with a null interval 
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return interval == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if(this.isBottom())
			return Lattice.bottomRepresentation();
		
		return new StringRepresentation("["+this.interval.getLow()+","+this.interval.getHigh()+"]");
	}

	@Override
	public int compareTo(Intervals o) {
		if(isBottom())
			return o.isBottom() ? 0 : -1; 
		if(isTop())
			return o.isTop() ? 0 : 1;
		
		if(o.isBottom())
			return 1;
		
		if(isTop())
			return -1;
		
		return interval.compareTo(o.interval);
	}

	// logic for evaluating expressions below
	
	@Override
	public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if(constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			Intervals singletonInterval = new Intervals(i,i);
			return singletonInterval;
		}
		
		return top();
	}

	@Override
	public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		
		
		if(left.isBottom() || right.isBottom())
			return bottom();
		
		IntInterval a = left.interval;
		IntInterval b = right.interval;
		
		if(operator instanceof AdditionOperator)  {
			
			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();
			
			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();
			
			return new Intervals(lA.add(lB), uA.add(uB));
			
		} else 
			
		// TODO: The semantics of other binary mathematical operations should be implemented here!
			
		if( operator instanceof SubtractionOperator) {
			
		} else if( operator instanceof MultiplicationOperator) {
			
			
		}
			
		return top();
	}

	@Override
	public int hashCode() {
		return Objects.hash(interval);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Intervals other = (Intervals) obj;
		return Objects.equals(interval, other.interval);
	}

	// logic for widening below
	
	@Override
	public Intervals wideningAux(
			Intervals other)
			throws SemanticException {
		MathNumber newLower, newUpper;
		if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
			//  high value is increasing 
			newUpper = MathNumber.PLUS_INFINITY;
		else
			newUpper = interval.getHigh();

		if (other.interval.getLow().compareTo(interval.getLow()) < 0)
			//  low value is decreasing
			newLower = MathNumber.MINUS_INFINITY;
		else
			newLower = interval.getLow();

		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : new Intervals(newLower, newUpper);
	}
	
	// logic for narrowing below
	
	@Override
	public Intervals narrowingAux(
			Intervals other)
			throws SemanticException {
		MathNumber newLow, newHigh;
		newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
		newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
		return new Intervals(newLow, newHigh);
	}
	
	
	@Override
	public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
			BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
			SemanticOracle oracle) throws SemanticException {
		
		// Any assumptions should be implemented here!
		
		return BaseNonRelationalValueDomain.super.assumeBinaryExpression(environment, operator, left, right, src, dest, oracle);
	}
	

	
}
