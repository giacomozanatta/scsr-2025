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
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
//import it.unive.lisa.util.numeric.IntInterval;
import it.unive.scsr.CustomIntInterval;
import it.unive.scsr.CustomMathNumber;
import it.unive.scsr.CustomMathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import it.unive.scsr.FloatInterval;

//import static it.unive.lisa.util.numeric.IntInterval.MINUS_ONE;

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
	public final CustomIntInterval interval;
	
	/**
	 * The abstract zero ({@code [0, 0]}) element.
	 */
	public static final Intervals ZERO = new Intervals(CustomIntInterval.ZERO);

	/**
	 * The abstract top ({@code [-Inf, +Inf]}) element.
	 */
	public static final Intervals TOP = new Intervals(CustomIntInterval.INFINITY);

	/**
	 * The abstract bottom element.
	 */
	public static final Intervals BOTTOM = new Intervals(null);

	/**
	 * Builds the interval.
	 * 
	 * @param interval the underlying {@link CustomIntInterval}
	 */
	public Intervals(
			CustomIntInterval interval) {
		this.interval = interval;
	}

	/**
	 * Builds the interval.
	 * 
	 * @param lower  the lower bound
	 * @param upper the higher bound
	 */
	public Intervals(
			CustomMathNumber lower,
			CustomMathNumber upper) {
		this(new CustomIntInterval(lower, upper));
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
		this(new CustomIntInterval(low, high));
	}

	/**
	 * Builds the top interval.
	 */
	public Intervals() {
		this(CustomIntInterval.INFINITY);
	}
	
	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {

		if (arg.isBottom())
			return bottom();

		if(operator instanceof NegatableOperator || operator instanceof NumericNegation) {
            return new Intervals(arg.interval.mul(new CustomIntInterval(-1,-1)));
		}
		
		return  top();
	}
	
	@Override
	public Intervals glbAux(Intervals other) throws SemanticException {
		
		CustomIntInterval a = this.interval;
		CustomIntInterval b = other.interval;
		
		CustomMathNumber lA = a.getLow();
		CustomMathNumber lB = b.getLow();
		
		CustomMathNumber uA = a.getHigh();
		CustomMathNumber uB = b.getHigh();
		
		if(lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
			return BOTTOM;
		
		CustomMathNumber newLower = lA.max(lB);
		CustomMathNumber newUpper = uA.min(uB);
		
		Intervals newInterval = new Intervals(newLower, newUpper);
		
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
	}

	@Override
	public Intervals lubAux(Intervals other) throws SemanticException {
		
		CustomIntInterval a = this.interval;
		CustomIntInterval b = other.interval;
		
		CustomMathNumber lA = a.getLow();
		CustomMathNumber lB = b.getLow();
		
		CustomMathNumber uA = a.getHigh();
		CustomMathNumber uB = b.getHigh();
		
		CustomMathNumber newLower = lA.min(lB);
		CustomMathNumber newUpper = uA.max(uB);
		
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
		if(constant.getValue() instanceof Float){
			Float f = (Float) constant.getValue();
			Integer i = (int) Math.ceil(f.floatValue());
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
		
		CustomIntInterval a = left.interval;
		CustomIntInterval b = right.interval;
		
		if(operator instanceof AdditionOperator)  {
			
			CustomMathNumber lA = a.getLow();
			CustomMathNumber lB = b.getLow();
			
			CustomMathNumber uA = a.getHigh();
			CustomMathNumber uB = b.getHigh();

			CustomMathNumber first = lA.add(lB);
			CustomMathNumber second = uA.add(uB);
			
			return new Intervals(first.min(second), first.max(second));
			
		} else if (operator instanceof SubtractionOperator) {

			CustomMathNumber lA = a.getLow();
			CustomMathNumber lB = b.getLow();

			CustomMathNumber uA = a.getHigh();
			CustomMathNumber uB = b.getHigh();

			CustomMathNumber newLow  = lA.subtract(uB); // NOTA: lA - uB
			CustomMathNumber newHigh = uA.subtract(lB); // NOTA: uA - lB

			return new Intervals(newLow, newHigh);

	} else if( operator instanceof MultiplicationOperator) {

			CustomMathNumber lA = a.getLow();
			CustomMathNumber lB = b.getLow();

			CustomMathNumber uA = a.getHigh();
			CustomMathNumber uB = b.getHigh();

			CustomMathNumber first = lA.multiply(lB);
			CustomMathNumber second = lA.multiply(uB);
			CustomMathNumber third = uA.multiply(lB);
			CustomMathNumber fourth = uA.multiply(uB);

			CustomMathNumber min = first.min(second).min(third).min(fourth);
			CustomMathNumber max = first.max(second).max(third).max(fourth);

			return new Intervals(min, max);
		} else if (operator instanceof DivisionOperator){

			CustomMathNumber lA = a.getLow();
			CustomMathNumber lB = b.getLow();

			CustomMathNumber uA = a.getHigh();
			CustomMathNumber uB = b.getHigh();

			CustomMathNumber first = lA.divide(lB);
			CustomMathNumber second = lA.divide(uB);
			CustomMathNumber third = uA.divide(lB);
			CustomMathNumber fourth = uA.divide(uB);

			CustomMathNumber min = first.min(second).min(third).min(fourth);
			CustomMathNumber max = first.max(second).max(third).max(fourth);

			return new Intervals(min, max);
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
		CustomMathNumber newLower, newUpper;
		if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
			//  high value is increasing 
			newUpper = CustomMathNumber.PLUS_INFINITY;
		else
			newUpper = interval.getHigh();

		if (other.interval.getLow().compareTo(interval.getLow()) < 0)
			//  low value is decreasing
			newLower = CustomMathNumber.MINUS_INFINITY;
		else
			newLower = interval.getLow();

		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : new Intervals(newLower, newUpper);
	}
	
	// logic for narrowing below
	
	@Override
	public Intervals narrowingAux(
			Intervals other)
			throws SemanticException {
		CustomMathNumber newLow, newHigh;
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
