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
import it.unive.lisa.symbolic.value.operator.*;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.helpers.FloatInterval;

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
	public final FloatInterval interval;
	
	/**
	 * The abstract zero ({@code [0, 0]}) element.
	 */
	public static final Intervals ZERO = new Intervals(FloatInterval.ZERO);

	/**
	 * The abstract top ({@code [-Inf, +Inf]}) element.
	 */
	public static final Intervals TOP = new Intervals(FloatInterval.INFINITY);

	/**
	 * The abstract bottom element.
	 */
	public static final Intervals BOTTOM = new Intervals(null);

	/**
	 * Builds the interval.
	 * 
	 * @param interval the underlying {@link FloatInterval}
	 */
	public Intervals(
			FloatInterval interval) {
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
		this(new FloatInterval(lower, upper));
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
		this(new FloatInterval(low, high));
	}

	/**
	 * Builds the interval.
	 *
	 * @param low  the lower bound
	 * @param high the higher bound
	 */
	public Intervals(
			float low,
			float high) {
		this(new FloatInterval(low, high));
	}

	/**
	 * Builds the top interval.
	 */
	public Intervals() {
		this(FloatInterval.INFINITY);
	}
	
	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		
		if(operator instanceof NegatableOperator) {
			// Negating [a, b] produces [-b, -a]
			return new Intervals(arg.interval.mul(FloatInterval.MINUS_ONE));
		}
		
		return top();
	}
	
	@Override
	public Intervals glbAux(Intervals other) throws SemanticException {
		
		FloatInterval a = this.interval;
		FloatInterval b = other.interval;
		
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
		
		FloatInterval a = this.interval;
		FloatInterval b = other.interval;
		
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
		} else if (constant.getValue() instanceof Float) {
			Float i = (Float) constant.getValue();
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
		
		FloatInterval a = left.interval;
		FloatInterval b = right.interval;
		
		if(operator instanceof AdditionOperator)  {
			
			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();
			
			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();
			
			return new Intervals(lA.add(lB), uA.add(uB));
			
		} else 
			

		if( operator instanceof SubtractionOperator) {
			// Performing [a, b] - [c, d] should produce [a - d, b - c]
			// Example [2, 4] - [1, 3] = [-1, 3], [2, 4] - [-10, 6] = [-4, 14]
			return new Intervals(left.interval.diff(right.interval));
		} else if( operator instanceof MultiplicationOperator) {
			// Performing [a, b] * [c, d] should produce [min(...), max(...)] where ... are a*c, a*d, b*c, b*d
			// For example [2, 4] * [-3, 2] = [b * c, b * d] = [-12, 8]
			return new Intervals(left.interval.mul(right.interval));
		} else if( operator instanceof DivisionOperator) {
			// No error on zero
			return new Intervals(left.interval.div(right.interval, false, false));
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
	

	public boolean isZero(){
		return this == ZERO || compareTo(ZERO) == 0;
	}
}
