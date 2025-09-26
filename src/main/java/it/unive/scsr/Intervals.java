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
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class Intervals implements
		BaseNonRelationalValueDomain<Intervals>,
		Comparable<Intervals> {

	public final IntInterval interval;

	public static final Intervals ZERO = new Intervals(IntInterval.ZERO);
	public static final Intervals TOP = new Intervals(IntInterval.INFINITY);
	public static final Intervals BOTTOM = new Intervals(null);

	public Intervals(IntInterval interval) {
		this.interval = interval;
	}

	public Intervals(MathNumber lower, MathNumber upper) {
		this(new IntInterval(lower, upper));
	}

	public Intervals(int low, int high) {
		this(new IntInterval(low, high));
	}

	public Intervals() {
		this(IntInterval.INFINITY);
	}

	@Override
	public Intervals evalUnaryExpression(UnaryOperator operator, Intervals arg, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if (arg.isBottom())
			return bottom();

		if (operator instanceof NegatableOperator) {
			MathNumber negLow = MathNumber.ZERO.subtract(arg.interval.getHigh());
			MathNumber negHigh = MathNumber.ZERO.subtract(arg.interval.getLow());
			return new Intervals(negLow, negHigh);
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

		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
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

		if (lA.compareTo(uA) > 0 || lB.compareTo(uB) > 0)
			return BOTTOM;

		Intervals newInterval = new Intervals(newLower, newUpper);
		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : newInterval;
	}

	@Override
	public boolean lessOrEqualAux(Intervals other) throws SemanticException {
		return other.interval.includes(this.interval);
	}

	@Override
	public Intervals top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return interval != null && interval.isInfinity();
	}

	@Override
	public Intervals bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return interval == null;
	}

	@Override
	public StructuredRepresentation representation() {
		if (this.isBottom())
			return Lattice.bottomRepresentation();

		return new StringRepresentation("[" + this.interval.getLow() + "," + this.interval.getHigh() + "]");
	}

	@Override
	public int compareTo(Intervals o) {
		if (isBottom())
			return o.isBottom() ? 0 : -1;
		if (isTop())
			return o.isTop() ? 0 : 1;

		if (o.isBottom())
			return 1;

		if (isTop())
			return -1;

		return interval.compareTo(o.interval);
	}

	@Override
	public Intervals evalNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return new Intervals(i, i);
		}

		return top();
	}

	@Override
	public Intervals evalBinaryExpression(BinaryOperator operator, Intervals left, Intervals right, ProgramPoint pp,
										  SemanticOracle oracle) throws SemanticException {

		if (left.isBottom() || right.isBottom())
			return bottom();

		IntInterval a = left.interval;
		IntInterval b = right.interval;

		if (operator instanceof AdditionOperator) {
			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();
			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();
			return new Intervals(lA.add(lB), uA.add(uB));

		} else if (operator instanceof SubtractionOperator) {
			MathNumber lA = a.getLow();
			MathNumber uB = b.getHigh();
			MathNumber uA = a.getHigh();
			MathNumber lB = b.getLow();
			return new Intervals(lA.subtract(uB), uA.subtract(lB));

		} else if (operator instanceof MultiplicationOperator) {
			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();
			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();

			MathNumber p1 = lA.multiply(lB);
			MathNumber p2 = lA.multiply(uB);
			MathNumber p3 = uA.multiply(lB);
			MathNumber p4 = uA.multiply(uB);

			MathNumber min = p1.min(p2).min(p3).min(p4);
			MathNumber max = p1.max(p2).max(p3).max(p4);

			return new Intervals(min, max);

		} else if (operator instanceof DivisionOperator) {
			if (b.getLow().compareTo(MathNumber.ZERO) <= 0 && b.getHigh().compareTo(MathNumber.ZERO) >= 0)
				return top(); // Division by zero

			MathNumber lA = a.getLow();
			MathNumber lB = b.getLow();
			MathNumber uA = a.getHigh();
			MathNumber uB = b.getHigh();

			MathNumber d1 = lA.divide(lB);
			MathNumber d2 = lA.divide(uB);
			MathNumber d3 = uA.divide(lB);
			MathNumber d4 = uA.divide(uB);

			MathNumber min = d1.min(d2).min(d3).min(d4);
			MathNumber max = d1.max(d2).max(d3).max(d4);

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
		if (obj == null || getClass() != obj.getClass())
			return false;
		Intervals other = (Intervals) obj;
		return Objects.equals(interval, other.interval);
	}

	@Override
	public Intervals wideningAux(Intervals other) throws SemanticException {
		MathNumber newLower, newUpper;
		if (other.interval.getHigh().compareTo(interval.getHigh()) > 0)
			newUpper = MathNumber.PLUS_INFINITY;
		else
			newUpper = interval.getHigh();

		if (other.interval.getLow().compareTo(interval.getLow()) < 0)
			newLower = MathNumber.MINUS_INFINITY;
		else
			newLower = interval.getLow();

		return newLower.isMinusInfinity() && newUpper.isPlusInfinity() ? top() : new Intervals(newLower, newUpper);
	}

	@Override
	public Intervals narrowingAux(Intervals other) throws SemanticException {
		MathNumber newLow = interval.getLow().isInfinite() ? other.interval.getLow() : interval.getLow();
		MathNumber newHigh = interval.getHigh().isInfinite() ? other.interval.getHigh() : interval.getHigh();
		return new Intervals(newLow, newHigh);
	}

	@Override
	public ValueEnvironment<Intervals> assumeBinaryExpression(ValueEnvironment<Intervals> environment,
															  BinaryOperator operator, ValueExpression left, ValueExpression right, ProgramPoint src, ProgramPoint dest,
															  SemanticOracle oracle) throws SemanticException {
		return BaseNonRelationalValueDomain.super.assumeBinaryExpression(environment, operator, left, right, src, dest, oracle);
	}

	// Division-by-zero helper method
	public boolean mayIncludeZero() {
		if (isBottom())
			return false;
		return interval.getLow().compareTo(MathNumber.ZERO) <= 0
				&& interval.getHigh().compareTo(MathNumber.ZERO) >= 0;
	}
}
